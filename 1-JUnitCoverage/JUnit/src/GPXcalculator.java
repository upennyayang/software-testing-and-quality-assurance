/**
 * This class contains static methods used for performing statistics and
 * other calculations on GPX data.
*/


public class GPXcalculator {

    public static final int R = 6371; // radius of the earth in km


    /**
     * Calculates the elapsed time for all segments in the track.
     * Note that it does not include the time <b>between</b> segments.
     *
     * @param trk The track for which to calculate the elapsed time.
     * @return the elapsed time in seconds; -1 if the track object is null
     */
    public static long calculateElapsedTime(GPXtrk trk) {

	if (trk == null) return -1;

	long time = 0;

	// iterate over all the segments and calculate the time for each
	GPXtrkseg trksegs[] = trk.trksegs();

	for (int i = 0; i < trksegs.length; i++) {
	    // keep a running total of the time for each segment
	    time += calculateElapsedTime(trksegs[i]);
	}
	
	return time;
    }

    /**
     * Calculates the elapsed time for the given segment by returning
     * the difference between the first and last track points.
     *
     * @param trkseg The track segment for which to calculate the elapsed time.
     * @return the elapsed time in seconds; -1 if the track segment object is null
     */
    public static long calculateElapsedTime(GPXtrkseg trkseg) {
	// get the time of the first point of the segment
	GPXtrkpt firstPt = trkseg.trkpt(0);
	GPXtime firstTime = firstPt.time();
	
	// get the time of the last point of the segment
	GPXtrkpt lastPt = trkseg.trkpt(trkseg.numPoints() - 1);
	GPXtime lastTime = lastPt.time();
	
	// convert the times to milliseconds so we can calculate the difference
	long start = firstTime.convertToJavaTime();
	long end = lastTime.convertToJavaTime();
	
	// total elapsed time in milliseconds
	return end - start;

    }

    /**
     * Calculates the distance traveled over the given segment by returning
     * the sum of the distances between successive track points.
     * The distance takes into account elevation, latitude, and longitude.
     * To account for the curvature of the earth, the spherical law of cosines
     * is used, based on http://www.movable-type.co.uk/scripts/latlong.html
     *
     * @param trkseg The track segment for which to calculate the distance traveled.
     * @return the total distance in meters
     */
    public static double calculateDistanceTraveled(GPXtrkseg trkseg) {

	double totalDistance = 0;
	
	// iterate over all the trkpts
	GPXtrkpt pts[] = trkseg.trkpts();

	for (int j = 0; j < pts.length-1; j++) {
	    
	    // get this point and the next one
	    GPXtrkpt pt1 = pts[j];
	    GPXtrkpt pt2 = pts[j+1];
	    
	    // convert lat and lon from degrees to radians
	    double lat1 = pt1.lat() * 2 * Math.PI / 360.0;
	    double lon1 = pt1.lon() * 2 * Math.PI / 360.0;
	    double lat2 = pt2.lat() * 2 * Math.PI / 360.0;
	    double lon2 = pt2.lon() * 2 * Math.PI / 360.0;
	    
	    // use the spherical law of cosines to figure out 2D distance
	    double d = Math.acos(Math.sin(lat1)*Math.sin(lat2) + Math.cos(lat1)*Math.cos(lat2)*Math.cos(lon2-lon1)) * R;	
	    // now we need to take the change in elevation into account
	    double ele1 = pt1.ele();
	    double ele2 = pt2.ele();
	    
	    // calculate the 3D distance
	    double distance = d + Math.abs(ele1-ele2);
	    
	    // add it to the running total
	    totalDistance += distance;
	    
	}
	
	return totalDistance;

    }


    /**
     * Calculates the distance traveled over all segments in the specified
     * track by returning the sum of the distances for each track segment.
     *
     * @param trk The track for which to calculate the distance traveled.
     * @return the total distance in meters
     */
    public static double calculateDistanceTraveled(GPXtrk trk) {
	
	double totalDistance = 0;

	// iterate over all the trksegs
	GPXtrkseg segs[] = trk.trksegs();

	for (int i = 0; i < segs.length; i++) {
	    // calculate the distance for this segment
	    totalDistance += calculateDistanceTraveled(segs[i]);
	}

	return totalDistance;


    }

    /**
     * Calculate the average speed over the entire track by determining
     * the distance traveled and the total time for each segment.
     *
     * @param trk The track for which to calculate the average speed
     * @return the average speed in meters per second.
     */
    public static double calculateAverageSpeed(GPXtrk trk) {

	// figure out the elapsed time in milliseconds
	long time = calculateElapsedTime(trk);

	// figure out the distance in kilometers
	double distance = calculateDistanceTraveled(trk);

	// return the average in meters/second
	return distance/time;

    }


    /**
     * Calculate the bearing (direction) from the first point in the
     * track to the last point in the track, using the bearing calculation
     * from http://www.movable-type.co.uk/scripts/latlong.html
     *
     * @param trk The track for which to calculate the overall bearing.
     * @return the bearing in degrees
     */
    public static double calculateBearing(GPXtrk trk) {

	// get the first trkpt in the first trkseg
	GPXtrkpt start = trk.trkseg(0).trkpt(0);
	// get the last trkpt in the last trkseg
	GPXtrkseg lastSeg = trk.trkseg(trk.numSegments()-1);
	GPXtrkpt end = lastSeg.trkpt(lastSeg.numPoints()-1);

	// get the points and convert to radians
	double lat1 = start.lat() * 2 * Math.PI / 360.0;
	double lon1 = start.lon() * 2 * Math.PI / 360.0;
	double lat2 = end.lat() * 2 * Math.PI / 360.0;
	double lon2 = end.lon() * 2 * Math.PI / 360.0;

	// calculate the bearing (in radians)
	double y = Math.sin(lon1-lon2) * Math.cos(lat2);
	double x = Math.cos(lat1)*Math.sin(lat2) + Math.sin(lat1)*Math.cos(lat2)*Math.cos(lon1-lon2);
	
	// return the bearing (after converting to degrees)
	return Math.atan2(y, x) * 360.0 / (2 * Math.PI);

    }


    /**
     * Determines which track segment has the fastest average speed.
     *
     * @param trk The track for which to calculate the fastest segment.
     * @return the 0-based index of the fastest segment.
     */
    public static int calculateFastestSegment(GPXtrk trk) {

	GPXtrkseg trksegs[] = trk.trksegs();

	int fastestSegment = 0;
	double fastestTime = 0;

	for (int i = 0; i < trksegs.length; i++) {
	    if (calculateDistanceTraveled(trksegs[i])/calculateElapsedTime(trksegs[i]) >= fastestTime)
		fastestSegment = i;
	}

	return fastestSegment;

    }

}