package com.optiva.flows;

import com.optiva.charging.openapi.diameter.DiameterMessage;
import com.optiva.charging.openapi.diameter.DiameterMessageHeader;
import com.optiva.charging.openapi.diameter.avp.Avp;
import com.optiva.charging.openapi.diameter.avp.AvpCode;
import com.optiva.charging.openapi.diameter.common.enumeration.CommandCode;
import io.vertx.core.buffer.Buffer;

import java.time.ZonedDateTime;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.UUID;
import java.util.concurrent.ThreadLocalRandom;
import java.util.function.BiFunction;
import java.util.function.Supplier;

import static com.optiva.charging.openapi.diameter.common.enumeration.AvpCodeTable.RFC.AUTH_APPLICATION_ID;
import static com.optiva.charging.openapi.diameter.common.enumeration.AvpCodeTable.RFC.CALLED_STATION_ID;
import static com.optiva.charging.openapi.diameter.common.enumeration.AvpCodeTable.RFC.CC_INPUT_OCTETS;
import static com.optiva.charging.openapi.diameter.common.enumeration.AvpCodeTable.RFC.CC_OUTPUT_OCTETS;
import static com.optiva.charging.openapi.diameter.common.enumeration.AvpCodeTable.RFC.CC_REQUEST_NUMBER;
import static com.optiva.charging.openapi.diameter.common.enumeration.AvpCodeTable.RFC.CC_REQUEST_TYPE;
import static com.optiva.charging.openapi.diameter.common.enumeration.AvpCodeTable.RFC.CC_TOTAL_OCTETS;
import static com.optiva.charging.openapi.diameter.common.enumeration.AvpCodeTable.RFC.DESTINATION_HOST;
import static com.optiva.charging.openapi.diameter.common.enumeration.AvpCodeTable.RFC.DESTINATION_REALM;
import static com.optiva.charging.openapi.diameter.common.enumeration.AvpCodeTable.RFC.EVENT_TIMESTAMP;
import static com.optiva.charging.openapi.diameter.common.enumeration.AvpCodeTable.RFC.MULTIPLE_SERVICES_CREDIT_CONTROL;
import static com.optiva.charging.openapi.diameter.common.enumeration.AvpCodeTable.RFC.MULTIPLE_SERVICES_INDICATOR;
import static com.optiva.charging.openapi.diameter.common.enumeration.AvpCodeTable.RFC.ORIGIN_HOST;
import static com.optiva.charging.openapi.diameter.common.enumeration.AvpCodeTable.RFC.ORIGIN_REALM;
import static com.optiva.charging.openapi.diameter.common.enumeration.AvpCodeTable.RFC.RATING_GROUP;
import static com.optiva.charging.openapi.diameter.common.enumeration.AvpCodeTable.RFC.REQUESTED_SERVICE_UNIT;
import static com.optiva.charging.openapi.diameter.common.enumeration.AvpCodeTable.RFC.SERVICE_CONTEXT_ID;
import static com.optiva.charging.openapi.diameter.common.enumeration.AvpCodeTable.RFC.SESSION_ID;
import static com.optiva.charging.openapi.diameter.common.enumeration.AvpCodeTable.RFC.SUBSCRIPTION_ID;
import static com.optiva.charging.openapi.diameter.common.enumeration.AvpCodeTable.RFC.SUBSCRIPTION_ID_DATA;
import static com.optiva.charging.openapi.diameter.common.enumeration.AvpCodeTable.RFC.SUBSCRIPTION_ID_TYPE;
import static com.optiva.charging.openapi.diameter.common.enumeration.AvpCodeTable.RFC.USED_SERVICE_UNIT;
import static com.optiva.charging.openapi.diameter.common.enumeration.AvpCodeTable.TGPP.PS_INFORMATION;
import static com.optiva.charging.openapi.diameter.common.enumeration.AvpCodeTable.TGPP.REPORTING_REASON;
import static com.optiva.charging.openapi.diameter.common.enumeration.AvpCodeTable.TGPP.SERVICE_INFORMATION;
import static com.optiva.charging.openapi.diameter.common.enumeration.AvpCodeTable.TGPP.TGPP_USER_LOCATION_INFO;
import static com.optiva.charging.openapi.diameter.common.enumeration.AvpCodeTable.RFC.RESULT_CODE; // Import RESULT_CODE
import static jakarta.xml.bind.DatatypeConverter.parseHexBinary;

public class DiameterFBC implements DiameterFlow {

    private enum FlowState {
        INITIAL, // Ready to send CCR-I
        UPDATE,  // Ready to send CCR-U
        TERMINATE, // Ready to send CCR-T
        COMPLETED, // Successfully finished
        FAILED     // Failed due to error response
    }

    private static final List<Avp> STATIC_AVPS = staticAvps();
    private final String msisdn;
    private final int ratingGroup;
    private final int totalMessagesInSequence; // e.g. if MainVerticle.fbcMessageCount is 4, this is 4. (I, U, U, T)
    private final String session;
    private final ThreadLocalRandom random = ThreadLocalRandom.current();

    private int currentMessageIndex; // 0 for CCR-I, 1 for first CCR-U, etc. up to totalMessagesInSequence - 1 for CCR-T
    private FlowState currentState;

    public DiameterFBC(String msisdn, int ratingGroup, int totalMessagesInSequence) {
        this.msisdn = msisdn;
        this.ratingGroup = ratingGroup;
        // totalMessagesInSequence is the "fbcMessageCount" from MainVerticle.
        // It represents the total number of messages in this flow instance (I, U..., T).
        this.totalMessagesInSequence = totalMessagesInSequence;
        this.session = UUID.randomUUID().toString();
        this.currentMessageIndex = 0; // Start with the first message (CCR-I)
        this.currentState = FlowState.INITIAL;
    }

    @Override
    public Buffer getNextMessage() {
        if (currentState == FlowState.INITIAL && currentMessageIndex == 0) {
            // This is CCR-I, CC_REQUEST_TYPE = 1
            return ccrMessage(1, true, 0);
        } else if (currentState == FlowState.UPDATE && currentMessageIndex > 0 && currentMessageIndex < totalMessagesInSequence - 1) {
            // This is CCR-U, CC_REQUEST_TYPE = 2
            // currentMessageIndex maps to CC_REQUEST_NUMBER (0-based for index, 0-based for req number in Diameter)
            return ccrMessage(2, true, 1000L);
        } else if (currentState == FlowState.TERMINATE && currentMessageIndex == totalMessagesInSequence - 1) {
            // This is CCR-T, CC_REQUEST_TYPE = 3
            return ccrMessage(3, false, 1000L);
        } else {
            // COMPLETED, FAILED, or inconsistent state
            return null;
        }
    }

    @Override
    public void processResponse(DiameterMessage responseMessage) {
        if (currentState == FlowState.COMPLETED || currentState == FlowState.FAILED) {
            // Flow already finished, ignore further responses (should not happen with proper scheduler)
            return;
        }

        // Check Result-Code AVP
        Integer resultCode = responseMessage.getAvpIntValue(RESULT_CODE);
        boolean isSuccess = resultCode != null && (resultCode >= 2000 && resultCode < 3000);

        if (!isSuccess) {
            currentState = FlowState.FAILED;
            // Optionally log the error details from responseMessage
            System.err.println("Flow " + session + " failed. Result-Code: " + resultCode);
            return;
        }

        // Successful response, advance state
        currentMessageIndex++; // Advance to next message in sequence

        if (currentState == FlowState.INITIAL) { // Response to CCR-I
            if (totalMessagesInSequence == 1) { // Only CCR-I and CCR-T (no CCR-U)
                 currentState = FlowState.TERMINATE; // This means totalMessagesInSequence was 1, which is unusual (I then T)
                                                  // Or if totalMessagesInSequence was 2 (I then T)
                                                  // If totalMessagesInSequence is 1, it's I, then T. currentMessageIndex becomes 1.
                                                  // If totalMessagesInSequence is 2, it's I, then U (no, T).
                                                  // Let's clarify fbcMessageCount:
                                                  // if fbcMessageCount = 1 (I), next should be T. So this becomes TERMINATE.
                                                  // if fbcMessageCount = 2 (I, T), currentMessageIndex is now 1 (for T). -> TERMINATE
                                                  // if fbcMessageCount = 3 (I, U, T), currentMessageIndex is now 1 (for U). -> UPDATE
                if (currentMessageIndex == totalMessagesInSequence -1) { // if next is T
                    currentState = FlowState.TERMINATE;
                } else if (currentMessageIndex < totalMessagesInSequence -1 ) { // if next is U
                    currentState = FlowState.UPDATE;
                } else { // Should be T if only I was sent.
                     currentState = FlowState.COMPLETED; // Or FAILED if this state is unexpected.
                }

            } else { // Has CCR-U messages
                currentState = FlowState.UPDATE;
            }
        } else if (currentState == FlowState.UPDATE) { // Response to CCR-U
            if (currentMessageIndex == totalMessagesInSequence - 1) { // All CCR-Us sent, next is CCR-T
                currentState = FlowState.TERMINATE;
            } else if (currentMessageIndex < totalMessagesInSequence - 1) { // More CCR-Us to send
                currentState = FlowState.UPDATE; // Stays in UPDATE state
            } else { // Should not happen: currentMessageIndex >= totalMessagesInSequence
                currentState = FlowState.COMPLETED; // Or FAILED
            }
        } else if (currentState == FlowState.TERMINATE) { // Response to CCR-T
            currentState = FlowState.COMPLETED;
        }
    }


    @Override
    public boolean isInitialized() {
        // A flow is "initialized" after its first message (CCR-I) has been processed.
        // This might mean currentMessageIndex > 0 or a specific state.
        // Let's consider it initialized if it's past the INITIAL state.
        return currentState != FlowState.INITIAL || currentMessageIndex > 0;
    }

    @Override
    public DiameterFlow terminate() {
        // This method could be used to forcefully move the flow to a state where it sends CCR-T next,
        // or mark it as FAILED/COMPLETED to stop further messages.
        // For now, let's make it move to a state ready to send CCR-T if not already there,
        // or directly to FAILED/COMPLETED if it's too early.
        if (currentState != FlowState.COMPLETED && currentState != FlowState.FAILED) {
            if (currentMessageIndex < totalMessagesInSequence -1 && totalMessagesInSequence > 0) {
                 this.currentState = FlowState.TERMINATE;
                 this.currentMessageIndex = totalMessagesInSequence - 1; // Set to send CCR-T
            } else if (totalMessagesInSequence == 0) { // no messages to send at all
                this.currentState = FlowState.COMPLETED;
            } else { // Already in TERMINATE state or beyond
                 this.currentState = FlowState.COMPLETED; // Or FAILED if appropriate
            }
        }
        return this;
    }

    @Override
    public String getKey() {
        return session;
    }

    @Override
    public DiameterFlow restart() {
        return new DiameterFBC(msisdn, ratingGroup, totalMessagesInSequence);
    }

    // Consolidated message creation
    private Buffer ccrMessage(int ccRequestType, boolean includeRequestedServiceUnit, long usedUnits) {
        DiameterMessageHeader header = headerSupplier.get();
        // currentMessageIndex is 0-based for CC_REQUEST_NUMBER
        List<Avp> avps = dynamicAvps(ccRequestType, includeRequestedServiceUnit, usedUnits, currentMessageIndex);
        return messageFunction.apply(header, avps);
    }

    private List<Avp> dynamicAvps(int requestType, boolean request, long usedUnits, int reqNum) {
        HashMap<AvpCode, Avp> msccValue = new HashMap<>();
        msccValue.put(RATING_GROUP, RATING_GROUP.createAvp(ratingGroup));
        if (request) {
            msccValue.put(REQUESTED_SERVICE_UNIT, REQUESTED_SERVICE_UNIT.createAvp());
        }
        if (usedUnits > 0) {
            msccValue.put(USED_SERVICE_UNIT,
                          USED_SERVICE_UNIT.createAvp(Map.of(CC_TOTAL_OCTETS,
                                                             CC_TOTAL_OCTETS.createAvp(usedUnits),
                                                             CC_INPUT_OCTETS,
                                                             CC_INPUT_OCTETS.createAvp(usedUnits / 2),
                                                             CC_OUTPUT_OCTETS,
                                                             CC_OUTPUT_OCTETS.createAvp(usedUnits / 2))));
            msccValue.put(REPORTING_REASON, REPORTING_REASON.createAvp(3));
        }
        return List.of(SESSION_ID.createAvp(session),
                       EVENT_TIMESTAMP.createAvp(ZonedDateTime.now()),
                       CC_REQUEST_NUMBER.createAvp(reqNum), // Use passed reqNum
                       CC_REQUEST_TYPE.createAvp(requestType),
                       SUBSCRIPTION_ID.createAvp(Map.of(SUBSCRIPTION_ID_TYPE,
                                                        SUBSCRIPTION_ID_TYPE.createAvp(0),
                                                        SUBSCRIPTION_ID_DATA,
                                                        SUBSCRIPTION_ID_DATA.createAvp(msisdn))),
                       MULTIPLE_SERVICES_CREDIT_CONTROL.createAvp(msccValue));
    }

    private final Supplier<DiameterMessageHeader> headerSupplier
            = () -> new DiameterMessageHeader.Builder(CommandCode.CC).setApplicationId(4)
            .setEndToEndId(random.nextLong())
            .setHopByHopId(random.nextLong())
            .setRequest()
            .setVersion((byte) 1)
            .build();

    // Changed BiFunction to return Vert.x Buffer
    private final BiFunction<DiameterMessageHeader, List<Avp>, Buffer> messageFunction = (header, dynamicAvps) -> {
        ArrayList<Avp> avps = new ArrayList<>(dynamicAvps.size() + STATIC_AVPS.size());
        avps.addAll(STATIC_AVPS);
        avps.addAll(dynamicAvps);

        return writeMessageToBuffer(new DiameterMessage(header, avps));
    };

    private static List<Avp> staticAvps() {
        return List.of(ORIGIN_HOST.createAvp("diameterclient"),
                       ORIGIN_REALM.createAvp("optiva-test"),
                       DESTINATION_HOST.createAvp("IoT"),
                       DESTINATION_REALM.createAvp("optiva"),
                       AUTH_APPLICATION_ID.createAvp(4),
                       SERVICE_CONTEXT_ID.createAvp("32251@3gpp.org"),
                       SERVICE_INFORMATION.createAvp(Map.of(PS_INFORMATION,
                                                            PS_INFORMATION.createAvp(Map.of(CALLED_STATION_ID,
                                                                                            CALLED_STATION_ID.createAvp(
                                                                                                    "iot.truphone.com"),
                                                                                            TGPP_USER_LOCATION_INFO,
                                                                                            TGPP_USER_LOCATION_INFO.createAvp(
                                                                                                    parseHexBinary(
                                                                                                            "0162f2102f4c6bb6")))))),
                       MULTIPLE_SERVICES_INDICATOR.createAvp(1));

    }
}
