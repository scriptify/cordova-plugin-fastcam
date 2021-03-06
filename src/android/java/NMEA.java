package com.cordovapluginfastcam;

import android.util.Log;


import java.util.HashMap;
import java.util.Map;

/**
 * From: https://gist.github.com/javisantana/1326141/30d6b5b603fa113d7a17bfcc0a8aaa25a107d581
 */
public class NMEA {

    interface SentenceParser {
        public boolean parse(String [] tokens, GPSPosition position);
    }

    // utils
    static double Latitude2Decimal(String lat, String NS) {
        if (lat.length() < 2) return 0f;
        double med = Double.parseDouble(lat.substring(2))/60.0f;
        med +=  Double.parseDouble(lat.substring(0, 2));
        if(NS.startsWith("S")) {
            med = -med;
        }
        return med;
    }

    static double Longitude2Decimal(String lon, String WE) {
        if (lon.length() < 3) return 0f;
        double med = Double.parseDouble(lon.substring(3))/60.0f;
        med +=  Double.parseDouble(lon.substring(0, 3));
        // double med = Double.parseDouble(lon);
        if(WE.startsWith("W")) {
            med = -med;
        }
        return med;
    }

    // parsers
    class GPGGA implements SentenceParser {
        public boolean parse(String [] tokens, GPSPosition position) {
            position.time = Double.parseDouble(tokens[1]);
            position.lat = Latitude2Decimal(tokens[2], tokens[3]);
            position.lon = Longitude2Decimal(tokens[4], tokens[5]);
            position.quality = Integer.parseInt(tokens[6]);
            // Subtract geoid separator from MSL, as it isn't precise
            // enough for our use case. We calculate it ourselves
            position.altitude = Double.parseDouble(tokens[9]);
            position.origAltitude = Double.parseDouble(tokens[9]);
            position.geoidSeparator = Double.parseDouble(tokens[11]);
            return true;
        }
    }

    class GPGGL implements SentenceParser {
        public boolean parse(String [] tokens, GPSPosition position) {
            position.lat = Latitude2Decimal(tokens[1], tokens[2]);
            position.lon = Longitude2Decimal(tokens[3], tokens[4]);
            position.time = Float.parseFloat(tokens[5]);
            return true;
        }
    }

    class GPRMC implements SentenceParser {
        public boolean parse(String [] tokens, GPSPosition position) {
            position.time = Float.parseFloat(tokens[1]);
            position.lat = Latitude2Decimal(tokens[3], tokens[4]);
            position.lon = Longitude2Decimal(tokens[5], tokens[6]);
            position.velocity = Float.parseFloat(tokens[7]);
            position.dir = Float.parseFloat(tokens[8]);
            return true;
        }
    }

    class GPVTG implements SentenceParser {
        public boolean parse(String [] tokens, GPSPosition position) {
            position.dir = Float.parseFloat(tokens[3]);
            return true;
        }
    }

    class GPRMZ implements SentenceParser {
        public boolean parse(String [] tokens, GPSPosition position) {
            position.altitude = Float.parseFloat(tokens[1]);
            return true;
        }
    }


    GPSPosition position = new GPSPosition();

    private static final Map<String, SentenceParser> sentenceParsers = new HashMap<String, SentenceParser>();

    public NMEA() {
        sentenceParsers.put("GGA", new GPGGA());
        sentenceParsers.put("GGL", new GPGGL());
        sentenceParsers.put("RMC", new GPRMC());
        sentenceParsers.put("RMZ", new GPRMZ());
        //only really good GPS devices have this sentence but ...
        sentenceParsers.put("VTG", new GPVTG());
    }

    public GPSPosition parse(String lines, String typeFilter) throws NullPointerException {
        String[] splitted = lines.split("\n");
        for (int i = 0; i < splitted.length; i++) {
            String line = splitted[i];
            if(line.startsWith("$")) {
                String nmea = line.substring(1);
                String[] tokens = nmea.split(",");
                if(tokens[0].length() < 2) {
                    continue;
                }
                String type = tokens[0].substring(2);

                if (!type.equals(typeFilter)) {
                    continue;
                }
                if(sentenceParsers.containsKey(type)) {
                    sentenceParsers.get(type).parse(tokens, position);
                }
                position.updatefix();
            }
        }

        return position;
    }
}