package com.dave.filestorage.db;

import org.springframework.data.annotation.Id;
import org.springframework.data.mongodb.core.index.Indexed;
import org.springframework.data.mongodb.core.mapping.Document;

import java.util.Date;
import java.util.Set;

/**
 * Stores the last-known set of object keys per bucket for Garage change-detection polling.
 */
@Document(collection = "garage_poll_state")
public class GaragePollState {

    @Id
    private String id;

    @Indexed(unique = true)
    private String bucket;

    private Set<String> objectKeys;
    private Date lastPolledAt;

    public GaragePollState() {}

    public GaragePollState(String bucket, Set<String> objectKeys, Date lastPolledAt) {
        this.bucket = bucket;
        this.objectKeys = objectKeys;
        this.lastPolledAt = lastPolledAt;
    }

    public String getId() { return id; }
    public void setId(String id) { this.id = id; }
    public String getBucket() { return bucket; }
    public void setBucket(String bucket) { this.bucket = bucket; }
    public Set<String> getObjectKeys() { return objectKeys; }
    public void setObjectKeys(Set<String> objectKeys) { this.objectKeys = objectKeys; }
    public Date getLastPolledAt() { return lastPolledAt; }
    public void setLastPolledAt(Date lastPolledAt) { this.lastPolledAt = lastPolledAt; }
}
