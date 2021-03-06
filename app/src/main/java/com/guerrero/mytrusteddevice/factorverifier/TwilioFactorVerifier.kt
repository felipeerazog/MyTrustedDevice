package com.guerrero.mytrusteddevice.factorverifier

import android.os.Build
import com.guerrero.mytrusteddevice.shared.AccessToken
import com.guerrero.mytrusteddevice.shared.ChallengeWrapper
import com.guerrero.mytrusteddevice.shared.RegisterInfo
import com.twilio.verify.TwilioVerify
import com.twilio.verify.models.*

private const val PAGE_SIZE = 20

class TwilioFactorVerifier(
    private val twilioVerify: TwilioVerify
) : FactorVerifier {

    override fun createFactor(
        accessToken: AccessToken,
        pushToken: String,
        onSuccess: (RegisterInfo) -> Unit,
        onError: (Exception) -> Unit
    ) {
        val factorPayload = buildFactorPayload(accessToken, pushToken)
        twilioVerify.createFactor(
            factorPayload,
            { factor -> verifyFactor(factor.sid, onSuccess, onError) },
            { exception -> onError(exception) }
        )
    }

    private fun verifyFactor(
        factorSid: String,
        onSuccess: (RegisterInfo) -> Unit,
        onError: (Exception) -> Unit
    ) {
        val verifyFactorPayload = VerifyPushFactorPayload(factorSid)
        twilioVerify.verifyFactor(
            verifyFactorPayload,
            { factor -> onSuccess(factor.toRegisterInfo()) },
            { exception -> onError(exception) }
        )
    }

    override fun updatePushToken(token: String) {
        twilioVerify.getAllFactors(
            { factors ->
                for (factor in factors) {
                    updateFactor(factor, token)
                }
            },
            { exception -> exception.printStackTrace() }
        )
    }

    private fun updateFactor(
        factor: Factor,
        token: String
    ) {
        twilioVerify.updateFactor(
            UpdatePushFactorPayload(factor.sid, token),
            {},
            { exception -> exception.printStackTrace() })
    }

    private fun buildFactorPayload(
        accessToken: AccessToken,
        pushToken: String
    ): PushFactorPayload {
        return accessToken.run {
            PushFactorPayload(
                friendlyName = Build.MODEL,
                serviceSid = serviceSid,
                identity = identity,
                pushToken = pushToken,
                accessToken = token
            )
        }
    }

    override fun getPendingChallenges(
        factorSid: String,
        onSuccess: (List<ChallengeWrapper>) -> Unit,
        onError: (Exception) -> Unit
    ) {
        twilioVerify.getAllChallenges(
            ChallengeListPayload(
                factorSid = factorSid,
                pageSize = PAGE_SIZE,
                status = ChallengeStatus.Pending,
                order = ChallengeListOrder.Desc
            ),
            { challengeList -> onSuccess(challengeList.toChallengeWrapperList()) },
            { exception -> onError(exception) }
        )
    }

    override fun getChallenge(
        challengeId: String,
        factorSid: String,
        onSuccess: (ChallengeWrapper) -> Unit,
        onError: (Exception) -> Unit
    ) {
        twilioVerify.getChallenge(challengeId, factorSid,
            { challenge ->
                onSuccess(challenge.toChallengeWrapper())
            },
            { exception -> onError(exception) }
        )
    }

    override fun approveChallenge(
        challengeId: String,
        factorSid: String,
        onSuccess: () -> Unit,
        onError: (Exception) -> Unit
    ) {
        twilioVerify.updateChallenge(
            UpdatePushChallengePayload(
                factorSid, challengeId, ChallengeStatus.Approved
            ),
            { onSuccess() },
            { exception -> onError(exception) }
        )
    }

    override fun denyChallenge(
        challengeId: String,
        factorSid: String,
        onSuccess: () -> Unit,
        onError: (Exception) -> Unit
    ) {
        twilioVerify.updateChallenge(
            UpdatePushChallengePayload(
                factorSid, challengeId, ChallengeStatus.Denied
            ),
            { onSuccess() },
            { exception -> onError(exception) }
        )
    }
}
