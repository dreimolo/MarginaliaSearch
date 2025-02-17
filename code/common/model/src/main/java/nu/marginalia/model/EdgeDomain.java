package nu.marginalia.model;

import lombok.*;

import javax.annotation.Nonnull;
import java.io.Serializable;
import java.util.Objects;
import java.util.function.Predicate;
import java.util.regex.Pattern;

@AllArgsConstructor
@Getter @Setter @Builder
public class EdgeDomain implements Serializable {

    @Nonnull
    public final String subDomain;
    @Nonnull
    public final String domain;

    @SneakyThrows
    public EdgeDomain(String host) {
        Objects.requireNonNull(host, "domain name must not be null");

        host = host.toLowerCase();

        var dot = host.lastIndexOf('.');

        if (dot < 0 || looksLikeAnIp(host)) { // IPV6 >.>
            subDomain = "";
            domain = host;
        }
        else {
            int dot2 = host.substring(0, dot).lastIndexOf('.');
            if (dot2 < 0) {
                subDomain = "";
                domain = host;
            }
            else {
                if (looksLikeGovTld(host))
                { // Capture .ac.jp, .co.uk
                    int dot3 = host.substring(0, dot2).lastIndexOf('.');
                    if (dot3 >= 0) {
                        dot2 = dot3;
                        subDomain = host.substring(0, dot2);
                        domain = host.substring(dot2 + 1);
                    }
                    else {
                        subDomain = "";
                        domain = host;
                    }
                }
                else {
                    subDomain = host.substring(0, dot2);
                    domain = host.substring(dot2 + 1);
                }
            }
        }
    }

    private static final Predicate<String> govListTest = Pattern.compile(".*\\.(id|ac|co|org|gov|edu|com)\\.[a-z]{2}").asMatchPredicate();
    private boolean looksLikeGovTld(String host) {
        if (host.length() < 8)
            return false;
        int cnt = 0;
        for (int i = host.length() - 7; i < host.length(); i++) {
            if (host.charAt(i) == '.')
                cnt++;
        }
        return cnt >= 2 && govListTest.test(host);
    }


    private static final Predicate<String> ipPatternTest = Pattern.compile("[\\d]{1,3}\\.[\\d]{1,3}\\.[\\d]{1,3}\\.[\\d]{1,3}").asMatchPredicate();

    private boolean looksLikeAnIp(String host) {
        if (host.length() < 7)
            return false;

        char firstChar = host.charAt(0);
        int lastChar = host.charAt(host.length() - 1);

        return Character.isDigit(firstChar)
                && Character.isDigit(lastChar)
                && ipPatternTest.test(host);
    }



    public EdgeUrl toRootUrl() {
        // Set default protocol to http, as most https websites redirect http->https, but few http websites redirect https->http
        return new EdgeUrl("http", this, null, "/", null);
    }

    public String toString() {
        return getAddress();
    }

    public String getAddress() {
        if (!subDomain.isEmpty()) {
            return subDomain + "." + domain;
        }
        return domain;
    }

    public String getDomainKey() {
        int cutPoint = domain.indexOf('.');
        if (cutPoint < 0) {
            return domain;
        }
        return domain.substring(0, cutPoint).toLowerCase();
    }

    public String getLongDomainKey() {
        StringBuilder ret = new StringBuilder();

        int cutPoint = domain.indexOf('.');
        if (cutPoint < 0) {
            ret.append(domain);
        }
        else {
            ret.append(domain, 0, cutPoint);
        }

        if (!"".equals(subDomain) && !"www".equals(subDomain)) {
            ret.append(":");
            ret.append(subDomain);
        }

        return ret.toString().toLowerCase();
    }


    public boolean hasSameTopDomain(EdgeDomain other) {
        if (other == null) return false;

        return domain.equalsIgnoreCase(other.domain);
    }

    public String getTld() {
        int dot = -1;
        int length = domain.length();

        if (ipPatternTest.test(domain)) {
            return "IP";
        }

        if (govListTest.test(domain)) {
            dot = domain.indexOf('.', Math.max(0, length - ".edu.uk".length()));
        }
        else {
            dot = domain.lastIndexOf('.');
        }


        if (dot < 0 || dot == domain.length() - 1) {
            return "-";
        }
        else {
            return domain.substring(dot + 1);
        }
    }

    public boolean equals(final Object o) {
        if (o == this) return true;
        if (!(o instanceof EdgeDomain other)) return false;
        final String this$subDomain = this.getSubDomain();
        final String other$subDomain = other.getSubDomain();
        if (!Objects.equals(this$subDomain,other$subDomain)) return false;
        final String this$domain = this.getDomain();
        final String other$domain = other.getDomain();
        if (!Objects.equals(this$domain,other$domain)) return false;
        return true;
    }

    public int hashCode() {
        final int PRIME = 59;
        int result = 1;
        final Object $subDomain = this.getSubDomain().toLowerCase();
        result = result * PRIME + $subDomain.hashCode();
        final Object $domain = this.getDomain().toLowerCase();
        result = result * PRIME + $domain.hashCode();
        return result;
    }

}
