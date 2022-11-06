package com.suburbs.council.election.enums;

import com.suburbs.council.election.utils.PaxosUtils;

/**
 * Response timing is used to categorize the members in terms
 * of their ability to quickly respond to the events.
 */
public enum ResponseTiming {

    IMMEDIATE{
        @Override
        public long getResponseDelay() {
            return 0L;
        }
    },
    MEDIUM{
        @Override
        public long getResponseDelay() {
            return PaxosUtils.generateRandomNumber(MEDIUM_MAX_DELAY, MEDIUM_MIN_DELAY);
        }
    },
    LATE{
        @Override
        public long getResponseDelay() {
            return PaxosUtils.generateRandomNumber(LATE_MAX_DELAY, LATE_MIN_DELAY);
        }
    },
    NEVER {
        @Override
        public long getResponseDelay() {
            return Long.MAX_VALUE;
        }
    };

    private static final int MEDIUM_MIN_DELAY = 200;
    private static final int MEDIUM_MAX_DELAY = 2_000;
    private static final int LATE_MIN_DELAY = 200;
    private static final int LATE_MAX_DELAY = 5_000;

    public abstract long getResponseDelay();
}
