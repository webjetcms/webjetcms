package sk.iway.iwcm.system.captcha;

import com.fasterxml.jackson.annotation.JsonProperty;

import java.util.List;

public class GoogleRecaptchaResponse {
    // "success": true|false,
    private boolean success;

    // "challenge_ts": timestamp,  // timestamp of the challenge load (ISO format yyyy-MM-dd'T'HH:mm:ssZZ)
    @JsonProperty(value = "challenge_ts")
    private String challengeDate;

    // "hostname": string,         // the hostname of the site where the reCAPTCHA was solved
    private String hostname;

    // "score": number             // the score for this request (0.0 - 1.0)
    private double score;

    // "action": string            // the action name for this request (important to verify)
    private String action;

    // "error-codes": [...] // optional
    @JsonProperty(value = "error-codes")
    private List<String> errorCodes;

    public boolean isSuccess() {
        return success;
    }

    public void setSuccess(boolean success) {
        this.success = success;
    }

    public String getChallengeDate() {
        return challengeDate;
    }

    public void setChallengeDate(String challengeDate) {
        this.challengeDate = challengeDate;
    }

    public String getHostname() {
        return hostname;
    }

    public void setHostname(String hostname) {
        this.hostname = hostname;
    }


    public double getScore() {
        return score;
    }

    public void setScore(double score) {
        this.score = score;
    }

    public String getAction() {
        return action;
    }

    public void setAction(String action) {
        this.action = action;
    }

    public List<String> getErrorCodes() {
        return errorCodes;
    }

    public void setErrorCodes(List<String> errorCodes) {
        this.errorCodes = errorCodes;
    }
}
