package io.ucoin.app.model;

import java.io.Serializable;
import java.util.Collection;

import io.ucoin.app.technical.ObjectUtils;
import io.ucoin.app.technical.crypto.CryptoUtils;
import io.ucoin.app.technical.crypto.KeyPair;

/**
 * A wallet is a user account
 * Created by eis on 13/01/15.
 */
public class Wallet extends KeyPair implements LocalEntity, Serializable {


    private Long id;
    private Long currencyId;
    private Long accountId;
    private String name;
    private Integer credit;
    private Identity identity;
    private Double creditAsUD;
    private Collection<WotCertification> certifications;

    /**
     * Use for UI, when some properties has not been displayed yet
     */
    private boolean isDirty = false;

    // TODO : voir si besoin de les garder ou pas
    private String salt;
    private String currency;

    public Wallet() {
        super(null, null);
        this.identity = new Identity();
    }

    public Wallet(String currency, String uid, byte[] pubKey, byte[] secKey) {
        super(pubKey, secKey);
        this.currency = currency;
        this.identity = new Identity();
        this.identity.setPubkey(pubKey == null ? null : CryptoUtils.encodeBase58(pubKey));
        this.identity.setUid(uid);
    }

    public Wallet(String currency, String uid, String pubKey, String secKey) {
        super(CryptoUtils.decodeBase58(pubKey), secKey == null ? null : CryptoUtils.decodeBase58(secKey));
        this.currency = currency;
        this.identity = new Identity();
        this.identity.setPubkey(pubKey);
        this.identity.setUid(uid);
    }

    public Wallet(String currency, byte[] secKey, Identity identity) {
        super(CryptoUtils.decodeBase58(identity.getPubkey()), secKey);
        this.currency = currency;
        this.identity = identity;
    }

    public Identity getIdentity() {
        return identity;
    }

    public void setIdentity(Identity identity) {
        this.identity = identity;
    }

    public String getPubKeyHash() {
        return identity.getPubkey();
    }

    public String getSalt(){
        return salt;
    }

    public void setSalt(String salt){
        this.salt = salt;
    }

    public String getCurrency() {
        return currency;
    }

    public void setCurrency(String currency) {
        this.currency = currency;
    }

    public boolean isAuthenticate() {
        return secretKey != null && identity != null && identity.getPubkey() != null;
    }

    public boolean isSelfSend() {
        return identity.getTimestamp() != -1;
    }

    public Long getCurrencyId() {
        return currencyId;
    }

    public void setCurrencyId(Long currencyId) {
        this.currencyId = currencyId;
    }

    public Long getAccountId() {
        return accountId;
    }

    public void setAccountId(Long accountId) {
        this.accountId = accountId;
    }

    public String getName() {
        return name;
    }

    public void setName(String name) {
        this.name = name;
    }

    public Integer getCredit() {
        return credit;
    }

    public void setCredit(Integer credit) {
        this.credit = credit;
    }

    @Override
    public Long getId() {
        return id;
    }

    @Override
    public void setId(Long id) {
        this.id = id;
    }

    public String toString() {
        return name;
    }

    public String getUid() {
        return identity.getUid();
    }

    public void setUid(String uid) {
        identity.setUid(uid);
    }

    public long getCertTimestamp() {
        return identity.getTimestamp();
    }

    public void setCertTimestamp(long timestamp) {
        identity.setTimestamp(timestamp);
    }

    public void setMember(Boolean isMember) {
        identity.setMember(isMember);
    }

    public Boolean getIsMember() {
        return identity.getIsMember();
    }

    public boolean isDirty() {
        return isDirty;
    }

    public void setDirty(boolean isDirty) {
        this.isDirty = isDirty;
    }

    public Double getCreditAsUD() {
        return creditAsUD;
    }

    public void setCreditAsUD(Double creditAsUD) {
        this.creditAsUD = creditAsUD;
    }

    public Collection<WotCertification> getCertifications() {
        return certifications;
    }

    public void setCertifications(Collection<WotCertification> certifications) {
        this.certifications = certifications;
    }

    @Override
    public boolean equals(Object o) {
        if (o instanceof Wallet) {
            return ObjectUtils.equals(id, ((Wallet)o).id)
                    && ObjectUtils.equals(getPubKeyHash(), ((Wallet)o).getPubKeyHash())
                    && ObjectUtils.equals(currencyId, ((Wallet)o).currencyId);
        }
        return super.equals(o);
    }
}
