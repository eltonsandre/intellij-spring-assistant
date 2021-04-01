package in.oneton.idea.spring.assistant.plugin.initializr.metadata.io.spring.initializr.util;


// Code borrowed from https://github.com/spring-io/initializr/blob/master/initializr-generator

/*
 * Copyright 2012-2017 the original author or authors.
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *      http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */


import lombok.AccessLevel;
import lombok.RequiredArgsConstructor;

/**
 * Define a {@link Version} range. A square bracket "[" or "]" denotes an inclusive
 * end of the range and a round bracket "(" or ")" denotes an exclusive end of the
 * range. A range can also be unbounded by defining a a single {@link Version}. The
 * examples below make this clear.
 * <p>
 * <ul>
 * <li>"[1.2.0.RELEASE,1.3.0.RELEASE)" version 1.2.0 and any version after
 * this, up to, but not including, version 1.3.0.</li>
 * <li>"(2.0.0.RELEASE,3.2.0.RELEASE]" any version after 2.0.0 up to and
 * including version 3.2.0.</li>
 * <li>"1.4.5.RELEASE", version 1.4.5 and all later versions.</li>
 * </ul>
 *
 * @author Stephane Nicoll
 */
@RequiredArgsConstructor(access = AccessLevel.PROTECTED)
public class VersionRange {

    final Version lowerVersion;
    final boolean lowerInclusive;
    final Version higherVersion;
    final boolean higherInclusive;

    public VersionRange(String rangeText) {
        VersionRange range = VersionParser.DEFAULT.parseRange(rangeText);
        this.lowerVersion = range.lowerVersion;
        this.lowerInclusive = range.lowerInclusive;
        this.higherVersion = range.higherVersion;
        this.higherInclusive = range.higherInclusive;
    }

    /**
     * Specify if the {@link Version} matches this range. Returns {@code true}
     * if the version is contained within this range, {@code false} otherwise.
     */
    public boolean match(Version version) {
        assert version != null;
        int lower = lowerVersion.compareTo(version);
        if (lower > 0) {
            return false;
        } else if (!lowerInclusive && lower == 0) {
            return false;
        }
        if (higherVersion != null) {
            int higher = higherVersion.compareTo(version);
            if (higher < 0) {
                return false;
            } else
                return higherInclusive || higher != 0;
        }
        return true;
    }

    public Version getLowerVersion() {
        return lowerVersion;
    }

    public boolean isLowerInclusive() {
        return lowerInclusive;
    }

    public Version getHigherVersion() {
        return higherVersion;
    }

    public boolean isHigherInclusive() {
        return higherInclusive;
    }

    @Override
    public String toString() {
        StringBuilder sb = new StringBuilder();
        if (lowerVersion != null) {
            sb.append(lowerInclusive ? ">=" : ">").append(lowerVersion);
        }
        if (higherVersion != null) {
            sb.append(" and ").append(higherInclusive ? "<=" : "<").append(higherVersion);
        }
        return sb.toString();
    }

    @Override
    public int hashCode() {
        final int prime = 31;
        int result = 1;
        result = prime * result + (higherInclusive ? 1231 : 1237);
        result = prime * result + ((higherVersion == null) ? 0 : higherVersion.hashCode());
        result = prime * result + (lowerInclusive ? 1231 : 1237);
        result = prime * result + ((lowerVersion == null) ? 0 : lowerVersion.hashCode());
        return result;
    }

    @Override
    public boolean equals(Object obj) {
        if (this == obj)
            return true;
        if (obj == null)
            return false;
        if (getClass() != obj.getClass())
            return false;
        VersionRange other = (VersionRange) obj;
        if (higherInclusive != other.higherInclusive)
            return false;
        if (higherVersion == null) {
            if (other.higherVersion != null)
                return false;
        } else if (!higherVersion.equals(other.higherVersion))
            return false;
        if (lowerInclusive != other.lowerInclusive)
            return false;
        if (lowerVersion == null) {
            return other.lowerVersion == null;
        } else
            return lowerVersion.equals(other.lowerVersion);
    }

}
