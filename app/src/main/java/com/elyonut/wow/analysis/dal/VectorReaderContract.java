package com.elyonut.wow.analysis.dal;

import android.provider.BaseColumns;

public final class VectorReaderContract {
    // To prevent someone from accidentally instantiating the contract class,
    // make the constructor private.
    private VectorReaderContract() {}

    /* Inner class that defines the table contents */
    public static class VectorEntry implements BaseColumns {
        public static final String TABLE_NAME = "entry";
        public static final String COLUMN_NAME_FEATURE_ID = "feature_id";
        public static final String COLUMN_NAME_MAX_LON = "max_lon";
        public static final String COLUMN_NAME_MIN_LON = "min_lon";
        public static final String COLUMN_NAME_MAX_LAT = "max_lat";
        public static final String COLUMN_NAME_MIN_LAT = "min_lat";
    }
}
