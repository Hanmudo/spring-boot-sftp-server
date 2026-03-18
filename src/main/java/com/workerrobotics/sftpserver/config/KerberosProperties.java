package com.workerrobotics.sftpserver.config;

import jakarta.validation.constraints.NotBlank;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.validation.annotation.Validated;

@ConfigurationProperties(prefix = "sftp.kerberos")
@Validated
public class KerberosProperties {

    /**
     * Zet Kerberos/GSSAPI aan of uit.
     */
    private boolean enabled = false;

    /**
     * Volledige service principal, bv. host/sftp.example.local@EXAMPLE.LOCAL
     */
    @NotBlank(message = "sftp.kerberos.service-principal is verplicht wanneer Kerberos actief is")
    private String servicePrincipal;

    /**
     * Pad naar keytab.
     */
    @NotBlank(message = "sftp.kerberos.keytab is verplicht wanneer Kerberos actief is")
    private String keytab;

    /**
     * Laat password-auth naast Kerberos toe.
     */
    private boolean passwordFallback = true;

    /**
     * Realm optional, vooral nuttig voor mapping/logging.
     */
    private String realm;

    public boolean isEnabled() {
        return enabled;
    }

    public void setEnabled(boolean enabled) {
        this.enabled = enabled;
    }

    public String getServicePrincipal() {
        return servicePrincipal;
    }

    public void setServicePrincipal(String servicePrincipal) {
        this.servicePrincipal = servicePrincipal;
    }

    public String getKeytab() {
        return keytab;
    }

    public void setKeytab(String keytab) {
        this.keytab = keytab;
    }

    public boolean isPasswordFallback() {
        return passwordFallback;
    }

    public void setPasswordFallback(boolean passwordFallback) {
        this.passwordFallback = passwordFallback;
    }

    public String getRealm() {
        return realm;
    }

    public void setRealm(String realm) {
        this.realm = realm;
    }
}
