package pt.ulisboa.tecnico.cmov.ubibike.domain;

/**
 * Created by Artur Fonseca on 13/05/2016.
 */
public final class Status {
    private static String s="Not riding";


    public static String getS() {
        return s;
    }

    public static void setS(String s) {
        Status.s = s;
    }
}

