package com.steelstory.identity.api;

/**
 * Current eligibility lookup; false means absent or disabled.
 * This read is not an ownership check or a lock for a later mutation.
 * Future atomic account-guard operations require a separate explicit contract.
 */
public interface AccountAccess {
    boolean isActive(AccountId accountId);
}
