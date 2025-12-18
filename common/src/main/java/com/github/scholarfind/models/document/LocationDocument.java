package com.github.scholarfind.models.document;

public record LocationDocument(
        LocationLevel locationLevel,
        PreferenceLevel preferenceLevel,

        String country,
        String state,
        String county,
        String city,
        String zip) {

    public LocationDocument {
        if (!validate()) {
            throw new IllegalArgumentException("Location level does not match the assigned location fields!");
        }
    }

    private boolean validate() {
        return switch (locationLevel) {
            case COUNTRY ->
                allPresent(country) && allNull(state, county, city, zip);

            case STATE ->
                allPresent(country, state) && allNull(county, city, zip);

            case COUNTY ->
                allPresent(country, state, county) && allNull(city, zip);

            case CITY ->
                allPresent(country, state, county, city) && allNull(zip);

            case ZIP ->
                allPresent(country, state, county, city, zip);
        };
    }

    private static boolean allPresent(final String... values) {
        for (String value : values) {
            if (value == null || value.isEmpty()) {
                return false;
            }
        }
        return true;
    }

    private static boolean allNull(final String... values) {
        for (String value : values) {
            if (value != null) {
                return false;
            }
        }
        return true;
    }

}
