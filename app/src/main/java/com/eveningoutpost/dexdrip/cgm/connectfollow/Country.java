package com.eveningoutpost.dexdrip.cgm.connectfollow;

import java.util.HashMap;
import java.util.Map;

public enum Country {

    Invalid("invalid"),
    Algeria("dz"),
    BosniaAndHerzegovina("ba"),
    Egypt("eg"),
    SouthAfrica("za"),
    Canada("ca"),
    CostaRica("cr"),
    Mexico("mx"),
    Morocco("ma"),
    Panama("pa"),
    PuertoRico("pr"),
    UnitedStates("us"),
    Argentina("ar"),
    Brazil("br"),
    Chile("cl"),
    Colombia("co"),
    Venezuela("ve"),
    HongKong("hk"),
    India("in"),
    Indonesia("id"),
    Israel("il"),
    Japan("jp"),
    Kuwait("kw"),
    Lebanon("lb"),
    Malaysia("my"),
    Philippines("ph"),
    Qatar("qa"),
    SaudiArabia("sa"),
    Singapore("sg"),
    SouthKorea("kr"),
    Taiwan("tw"),
    Thailand("th"),
    Tunisia("tn"),
    Turkey("tr"),
    UnitedArabEmirates("ae"),
    Vietnam("vn"),
    Austria("at"),
    Belgium("be"),
    Bulgaria("bg"),
    Croatia("hr"),
    CzechRepublic("cz"),
    Denmark("dk"),
    Estonia("ee"),
    Finland("fi"),
    France("fr"),
    Germany("de"),
    Greece("gr"),
    Hungary("hu"),
    Iceland("is"),
    Ireland("ie"),
    Italy("it"),
    Latvia("lv"),
    Lithuania("lt"),
    Luxembourg("lu"),
    Netherlands("nl"),
    Norway("no"),
    Poland("pl"),
    Portugal("pt"),
    Romania("ro"),
    Russia("ru"),
    Serbia("rs"),
    Slovakia("sk"),
    Slovenia("si"),
    Spain("es"),
    Sweden("se"),
    Switzerland("ch"),
    Ukraine("ua"),
    UnitedKingdom("gb"),
    Australia("au"),
    NewZealand("nz"),
    Bahrain("bh"),
    Oman("om"),
    China("cn"),
    Cyprus("cy"),
    Albania("al"),
    Armenia("am"),
    Azerbaijan("az"),
    Bahamas("bs"),
    Barbados("bb"),
    Belarus("by"),
    Bermuda("bm"),
    Bolivia("bo"),
    Cambodia("kh"),
    DominicanRepublic("do"),
    Ecuador("ec"),
    ElSalvador("sv"),
    Georgia("ge"),
    Guatemala("gt"),
    Honduras("hn"),
    Iran("ir"),
    Iraq("iq"),
    Jordan("jo"),
    Kosovo("xk"),
    Libya("ly"),
    Macau("mo"),
    Macedonia("mk"),
    Maldives("mv"),
    Malta("mt"),
    Mauritius("mu"),
    Mayotte("yt"),
    Moldova("md"),
    Montenegro("me"),
    Namibia("na"),
    NewCaledonia("nc"),
    Nicaragua("ni"),
    Nigeria("ng"),
    Pakistan("pk"),
    Paraguay("py"),
    StMartin("mf"),
    Sudan("sd"),
    Uruguay("uy"),
    Aruba("aw"),
    CaymanIslands("ky"),
    Curacao("cw"),
    Peru("pe");

    private static final Map<String, Country> mapToCode;

    static {
        mapToCode = new HashMap<>();

        for(Country cntry : values()) {
            mapToCode.put(cntry.countryCode, cntry);
        }
    }

    private String countryCode;

    private Country(String code){
        this.countryCode = code;
    }

    public String getCountryCode() {
        return this.countryCode;
    }

    public static Country getCountry(String code) {
        if(mapToCode.containsKey(code))
            return mapToCode.get(code);
        else
            return Invalid;
    }


}