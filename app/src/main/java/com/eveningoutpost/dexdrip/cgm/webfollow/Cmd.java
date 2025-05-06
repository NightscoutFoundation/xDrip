package com.eveningoutpost.dexdrip.cgm.webfollow;

import static com.eveningoutpost.dexdrip.models.JoH.emptyString;
import static com.eveningoutpost.dexdrip.cgm.webfollow.Agent.get;
import static com.eveningoutpost.dexdrip.utils.CipherUtils.getSHA256;

import com.eveningoutpost.dexdrip.models.BgReading;
import com.eveningoutpost.dexdrip.models.JoH;
import com.eveningoutpost.dexdrip.cgm.medtrum.BackfillAssessor;
import com.eveningoutpost.dexdrip.utils.DexCollectionType;

import org.json.JSONObject;

import java.nio.charset.StandardCharsets;
import java.text.ParseException;
import java.text.SimpleDateFormat;
import java.util.Locale;
import java.util.Map;
import java.util.Objects;
import java.util.TimeZone;

import lombok.Getter;
import lombok.RequiredArgsConstructor;
import lombok.val;
import okhttp3.MediaType;
import okhttp3.RequestBody;

/**
 * JamOrHam
 * Web Follow script command processor
 */

@SuppressWarnings("unchecked")
@RequiredArgsConstructor
public class Cmd {
    private static final String PUSH_METHOD = "push";
    private static final String FPUSH_METHOD = "fpush";
    private static final String CPUSH_METHOD = "cpush";
    private static final String JSON_METHOD = "jput";
    private static final String JSON_TYPE = "application/json";
    private static final MediaType JSON = MediaType.parse(JSON_TYPE);
    private final SimpleDateFormat format = new SimpleDateFormat("M/d/y h:m:s a", Locale.ENGLISH);
    private final ResponseGetter respondent = new ResponseGetterImpl();
    @Getter
    private final MContext m;

    {
        format.setTimeZone(TimeZone.getTimeZone("UTC"));
    }

    public Cmd processAll() {
        for (val cc : m.cmd) {
            if (m.shouldQuit()) break;
            if (m.shouldSkip()) {
                m.log("Skipping " + cc);
                continue;
            }
            m.log("Processing step: " + cc);
            val result = process(cc);
            if (!result) {
                m.log("FAILED AT STEP: " + cc);
                break;
            }
        }
        return this;
    }

    public boolean process(final String cmd) {
        try {
            val parts = cmd.split("\\|");
            val pc = parts[0].split(":");
            val p1 = parts[Math.min(parts.length - 1, 1)];
            val p1s = p1.split("\\^");
            val asn = p1s[p1s.length - 1].split(",");
            JSONObject json = null;

            switch (pc[0]) {
                case "action":
                    m.doAction(pc[1]);
                    break;

                case "allCurrent":
                    val last = BgReading.last();
                    if (last != null) {
                        if (JoH.msSince(last.timestamp) < DexCollectionType.getCurrentSamplePeriod()) {
                            m.doAction(pc[1]);
                        } else {
                            m.log("Is current");
                        }
                    } else {
                        m.log("No last reading");
                    }
                    return true;

                case "historyCurrent":
                    val bfp = BackfillAssessor.check();
                    if (bfp == null) {
                        m.doAction(pc[1]);
                    } else {
                        m.log("Backfill tripped: " + JoH.dateTimeText(bfp.first) + " -> " + JoH.dateTimeText(bfp.second));
                    }
                    return true;

                case "tSet":
                    if (m.token != null && m.token.length() > 5) {
                        m.doAction(pc[1]);
                    }
                    return true;

                case "vSet":
                    if (!JoH.emptyString(m.getByName(pc[2]))) {
                        m.doAction(pc[1]);
                    }
                    return true;

                case "vuSet":
                    if (JoH.emptyString(m.getByName(pc[2]))) {
                        m.doAction(pc[1]);
                    }
                    return true;

                case "evaluate":
                    evaluate(parts.length > 1 ? parts[1].split(";") : null);
                    return true;

                case "save":
                    m.save(pc[1]);
                    return true;

                case "error":
                    m.loge(pc[1]);
                    return true;
            }

            val cb = Config.builder()
                    .url(String.format(m.domain.replaceAll("/$", "")
                            + m.getByName(parts[0]), m.pid))
                    .agent(get(m.agent))
                    .version(m.version)
                    .product(m.product);

            switch (p1s[0]) {

                case FPUSH_METHOD:
                case CPUSH_METHOD:
                case PUSH_METHOD:
                    for (val as : asn) {
                        m.log("PUSH: as: " + as);
                        val ass = as.split("-", 2);
                        val tok = ass[1].split(";");
                        val tick = (tok[1].charAt(0) == '\'') ? tok[1].substring(1) : m.getByName(tok[1]); // handle string literal
                        if (tick == null) {
                            m.loge(tok[1] + " is null in " + as);
                            return false;
                        }
                        val mtick = mutate(tick, tok);
                        m.log("Setting: :" + ass[0] + ": to :" + String.format(tok[0], mtick));
                        if (p1s[0].equals(CPUSH_METHOD)) {
                            m.setByName(ass[0], String.format(tok[0], mtick));
                        } else {
                            cb.getClass().getMethod(ass[0], String.class).invoke(cb, String.format(tok[0], mtick));
                        }
                    }
                    if (!p1s[0].equals(PUSH_METHOD)) {
                        return true;
                    }
                    break;

                case JSON_METHOD:
                    json = new JSONObject();
                    for (val as : asn) {
                        val ass = as.split("-");
                        val cpf = Cpref.get(ass[1]);
                        if (emptyString(cpf)) {
                            m.loge("No preference value set for: " + ass[0] + " - cannot continue");
                            return false;
                        }
                        json.put(ass[0], cpf);
                    }
                    break;
            }

            if (json != null) {
                cb.body(RequestBody.create(JSON, json.toString()));
                cb.contentType(JSON_TYPE);
            }

            val cmdResponse = respondent.get(cb.build());
            if (cmdResponse == null || cmdResponse.length() < 5) {
                m.loge("Failed to get any response");
                return false;
            }

            val map = new Mapifier(cmdResponse);
            val p2s = parts[2].split(";");
            if (parts.length < 4) return extract(p2s, map);
            try {
                val presult = pprocess(parts[3].split(";"), map);
                if (!presult) {
                    m.loge("Pprocess failure " + parts[3] + " " + cmdResponse);
                } else {
                    return extract(p2s, map);
                }
                return presult;
            } catch (Exception e) {
                m.log("Pprocess exception: " + e);
                e.printStackTrace();
                return false;
            }
        } catch (com.google.gson.JsonSyntaxException e) {
            m.loge("Unable to parse: @" + cmd.split("\\|")[0] + " " + e);

        } catch (IllegalArgumentException e) {
            m.loge("Got exception 1: " + e);
        } catch (Exception e) {
            m.loge("Got exception 2: " + e);
            if (e instanceof RuntimeException) {
                if (e.getMessage() != null && e.getMessage().startsWith("401")) {
                    if (JoH.pratelimit("wf-response-401", 28800)) {
                        m.loge("Automatically start with refresh due to 401");
                        Cpref.startWithRefresh(true);
                    }
                }
                e.printStackTrace();
            } else {
                e.printStackTrace();
            }
        }
        return false;
    }

    private String mutate(String tick, final String[] t) throws NoSuchFieldException, IllegalAccessException {
        if (tick != null) {
            val a1 = "%sha256";
            if (t[0].contains(a1)) {
                t[0] = t[0].replace(a1, "%s");
                tick = getSHA256(tick.getBytes(StandardCharsets.UTF_8));
            }
        }
        return tick;
    }

    private boolean pprocess(final String[] p3s, final Mapifier map) {
        for (val psa : p3s) {
            val pss = psa.split("-");
            m.log("PProcessing psa: " + pss[0] + " -> " + pss[1]);
            try {
                switch (pss[1]) {
                    case "AZ":
                        val psd = map.pluckDouble(pss[0]);
                        val ret = psd == 0d;
                        m.log("AZ ret: " + ret);
                        if (!ret) {
                            if (psd > 0d) {
                                m.token = null;
                            }
                        }
                        return ret;
                    default:
                        m.loge("Unknown: " + pss[0]);
                }
            } catch (Exception e) {
                m.loge("pprocess exception: " + e);
                e.printStackTrace();
            }
        }
        return false;
    }

    private boolean extract(final String[] p2s, final Mapifier map) throws NoSuchFieldException, IllegalAccessException, ParseException {
        for (val psa : p2s) {
            val pss = psa.split("-");
            val type = pss[0].replaceAll("[a-z!]", "");
            val var = pss[0].replaceAll("[A-Z]", "");

            switch (type) {
                case "T":
                    m.setByName(var, Objects.requireNonNull(format.parse(map.pluckString(pss[1]))).getTime());
                    break;
                case "D":
                    m.setByName(var, map.pluckDouble(pss[1]));
                    break;
                case "L":
                    m.setByName(var, map.pluckAny(pss[1]));
                    m.log("Setting Any: " + var + " to " + map.pluckAny(pss[1]) + " <- " + var);
                    break;
                case "":
                    try {
                        val mvalue = map.pluckString(pss[1]);
                        if (mvalue == null || mvalue.length() < 2) {
                            m.log("Bad value: " + mvalue + " for " + pss[1]);
                            return var.endsWith("!");
                        }
                        m.setByName(var, mvalue);
                        m.log("Setting String: " + var + " to " + mvalue + " <- " + pss[1]);
                    } catch (RuntimeException e) {
                        val es = "" + e;
                        if (es.contains("Invalid")) {
                            m.loge(es);
                        }
                        throw e;
                    }
                    break;
                default:
                    throw new RuntimeException("Invalid type :" + type + ": <- " + var);
            }
        }
        return true;
    }

    private void evaluate(final String[] p2s) {
        try {
            m.evaluateSingle();
            if (m.history != null && p2s != null && p2s.length > 0) {
                for (val item : m.history) {
                    val map = new Mapifier((Map<String, Object>) item);
                    extract(p2s, map);
                    m.evaluateSingle();
                }
                m.history = null;
            }
        } catch (Exception e) {
            m.loge("Exception evaluating: " + e);
        }
    }
}
