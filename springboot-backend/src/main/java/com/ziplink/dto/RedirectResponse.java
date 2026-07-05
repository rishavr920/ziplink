package com.ziplink.dto;

import com.fasterxml.jackson.annotation.JsonInclude;

//RedirectResponse.
//Outgoing payload returned upon requesting redirect info: GET /api/redirect/{code}

@JsonInclude(JsonInclude.Include.NON_NULL)
public class RedirectResponse {

    private Boolean redirect;
    private String url;
    private Boolean requiresPassword;
    private String shortCode;

    public RedirectResponse() {}

    public RedirectResponse(Boolean redirect, String url, Boolean requiresPassword, String shortCode) {
        this.redirect = redirect;
        this.url = url;
        this.requiresPassword = requiresPassword;
        this.shortCode = shortCode;
    }

    public Boolean getRedirect() { return redirect; }
    public void setRedirect(Boolean redirect) { this.redirect = redirect; }

    public String getUrl() { return url; }
    public void setUrl(String url) { this.url = url; }

    public Boolean getRequiresPassword() { return requiresPassword; }
    public void setRequiresPassword(Boolean requiresPassword) { this.requiresPassword = requiresPassword; }

    public String getShortCode() { return shortCode; }
    public void setShortCode(String shortCode) { this.shortCode = shortCode; }

    public static RedirectResponseBuilder builder() { return new RedirectResponseBuilder(); }

    public static class RedirectResponseBuilder {
        private Boolean redirect;
        private String url;
        private Boolean requiresPassword;
        private String shortCode;

        public RedirectResponseBuilder redirect(Boolean redirect) {
            this.redirect = redirect;
            return this;
        }

        public RedirectResponseBuilder url(String url) {
            this.url = url;
            return this;
        }

        public RedirectResponseBuilder requiresPassword(Boolean requiresPassword) {
            this.requiresPassword = requiresPassword;
            return this;
        }

        public RedirectResponseBuilder shortCode(String shortCode) {
            this.shortCode = shortCode;
            return this;
        }

        public RedirectResponse build() {
            return new RedirectResponse(redirect, url, requiresPassword, shortCode);
        }
    }
}
