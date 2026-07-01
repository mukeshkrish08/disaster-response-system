package drs.shared.util;

/**
 * Great-circle distance between two GPS coordinates using the Haversine
 * formula. Pure function with no state - used by GeoDispatchService.
  
 */
public final class HaversineCalculator {

    private HaversineCalculator() {
        // Not instantiable
    }

    /*   * Compute the great-circle distance in kilometres between two points.
         * @param lat1 latitude of point A in degrees
     * @param lon1 longitude of point A in degrees
     * @param lat2 latitude of point B in degrees
     * @param lon2 longitude of point B in degrees
     * @return distance in kilometres (>= 0)
     */
    public static double calculateDistance(double lat1, double lon1,
                                           double lat2, double lon2) {
        double dLat = Math.toRadians(lat2 - lat1);
        double dLon = Math.toRadians(lon2 - lon1);
        double rLat1 = Math.toRadians(lat1);
        double rLat2 = Math.toRadians(lat2);

        double a = Math.sin(dLat / 2) * Math.sin(dLat / 2)
                + Math.cos(rLat1) * Math.cos(rLat2)
                * Math.sin(dLon / 2) * Math.sin(dLon / 2);
        double c = 2 * Math.atan2(Math.sqrt(a), Math.sqrt(1 - a));
        return DrsConstants.EARTH_RADIUS_KM * c;
    }
}
