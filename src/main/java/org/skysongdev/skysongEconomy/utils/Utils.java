package org.skysongdev.skysongEconomy.utils;

import org.skysongdev.skysongEconomy.data.datatypes.EconomyBusiness;

import java.util.Map;

public class Utils {
    private static Utils instance;
    public static Map<String, EconomyBusiness> economyBusinesses;

    public static Utils Instance(){
        if(instance == null){
            instance = new Utils();
        }
        return instance;
    }




}
