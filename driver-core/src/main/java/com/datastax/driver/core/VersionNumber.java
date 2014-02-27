/*
 *      Copyright (C) 2012 DataStax Inc.
 *
 *   Licensed under the Apache License, Version 2.0 (the "License");
 *   you may not use this file except in compliance with the License.
 *   You may obtain a copy of the License at
 *
 *      http://www.apache.org/licenses/LICENSE-2.0
 *
 *   Unless required by applicable law or agreed to in writing, software
 *   distributed under the License is distributed on an "AS IS" BASIS,
 *   WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 *   See the License for the specific language governing permissions and
 *   limitations under the License.
 */
package com.datastax.driver.core;

import java.util.Arrays;
import java.util.Collections;
import java.util.List;
import java.util.regex.Pattern;
import java.util.regex.Matcher;

import com.google.common.base.Objects;

/**
 * A version number in the form X.Y.Z with optional pre-release labels and build metadata.
 * <p>
 * Version numbers compare the usual way, the major number (X) is compared first, then the minor
 * one (Y) and then the patch level one (Z). Lastly, versions with pre-release sorts before the
 * versions that don't have one, and labels are sorted alphabetically if necessary. Build metadata
 * are ignored for sorting versions. The versions supported loosely correspond to what
 * http://semver.org/ defines though it does not adhere to it strictly.
 */
public class VersionNumber implements Comparable<VersionNumber> {

    private static final String VERSION_REGEXP = "(\\d+)\\.(\\d+)(\\.\\d+)?([~\\-]\\w[.\\w]*(?:\\-\\w[.\\w]*)*)?(\\+[.\\w]+)?";
    private static final Pattern pattern = Pattern.compile(VERSION_REGEXP);

    private final int major;
    private final int minor;
    private final int patch;

    private final String[] preReleases;
    private final String build;

    private VersionNumber(int major, int minor, int patch, String[] preReleases, String build) {
        this.major = major;
        this.minor = minor;
        this.patch = patch;
        this.preReleases = preReleases;
        this.build = build;
    }

    /**
     * Parse a version from a string.
     * <p>
     * The version string should have primarily the form X.Y.Z to which can be appended
     * one or more pre-release label after dashes (2.0.1-beta1, 2.1.4-rc1-SNAPSHOT)
     * and an optional build label (2.1.0-beta1+a20ba.sha). Out of convenience, the
     * "patch" version number, Z, can be ommitted, in which case it is assumed to be 0.
     *
     * @param version the string to parse
     * @throws IllegalArgumentException if the provided string does not
     * represent a valid version.
     */
    public static VersionNumber parse(String version) {
        if (version == null)
            return null;

        Matcher matcher = pattern.matcher(version);
        if (!matcher.matches())
            throw new IllegalArgumentException("Invalid version number: " + version);

        try {
            int major = Integer.parseInt(matcher.group(1));
            int minor = Integer.parseInt(matcher.group(2));

            String pa = matcher.group(3);
            int patch = pa == null || pa.isEmpty() ? 0 : Integer.parseInt(pa.substring(1)); // dropping the initial '.' since it's included this time

            String pr = matcher.group(4);
            String[] preReleases = pr == null || pr.isEmpty() ? null : pr.substring(1).split("\\-"); // drop initial '-' or '~' then split on the remaining ones

            String bl = matcher.group(5);
            String build = bl == null || bl.isEmpty() ? null : bl.substring(1); // drop the initial '+'

            return new VersionNumber(major, minor, patch, preReleases, build);
        } catch (NumberFormatException e) {
            throw new IllegalArgumentException("Invalid version number: " + version);
        }
    }

    /**
     * The major version number.
     *
     * @return the major version number, i.e. X in X.Y.Z.
     */
    public int getMajor() {
        return major;
    }

    /**
     * The minor version number.
     *
     * @return the minor version number, i.e. Y in X.Y.Z.
     */
    public int getMinor() {
        return minor;
    }

    /**
     * The patch version number.
     *
     * @return the patch version number, i.e. Z in X.Y.Z.
     */
    public int getPatch() {
        return patch;
    }

    /**
     * The pre-release labels if relevants, i.e. label1 and label2 in X.Y.Z-label1-lable2.
     *
     * @return the pre-releases labels. The return list will be {@code null} if the version number
     * doesn't have one.
     */
    public List<String> getPreReleaseLabels() {
        return Collections.unmodifiableList(Arrays.asList(preReleases));
    }

    /**
     * The build label if there is one.
     *
     * @return the build label or {@code null} if the version number
     * doesn't have one.
     */
    public String getBuildLabel() {
        return build;
    }

    public int compareTo(VersionNumber other) {
        if (major < other.major)
            return -1;
        if (major > other.major)
            return 1;

        if (minor < other.minor)
            return -1;
        if (minor > other.minor)
            return 1;

        if (patch < other.patch)
            return -1;
        if (patch > other.patch)
            return 1;

        if (preReleases == null)
            return other.preReleases == null ? 0 : 1;
        if (other.preReleases == null)
            return -1;

        for (int i = 0; i < Math.min(preReleases.length, other.preReleases.length); i++) {
            int cmp = preReleases[i].compareTo(other.preReleases[i]);
            if (cmp != 0)
                return cmp;
        }

        return preReleases.length == other.preReleases.length ? 0 : (preReleases.length < other.preReleases.length ? -1 : 1);
    }

    @Override
    public boolean equals(Object o) {
        if(!(o instanceof VersionNumber))
            return false;
        VersionNumber that = (VersionNumber)o;
        if (major != that.major || minor != that.minor || patch != that.patch)
            return false;

        return major == that.major
            && minor == that.minor
            && patch == that.patch
            && (preReleases == null ? that.preReleases == null : Arrays.equals(preReleases, that.preReleases))
            && Objects.equal(build, that.build);
    }

    @Override
    public int hashCode() {
        return Objects.hashCode(major, minor, patch, preReleases, build);
    }

    @Override
    public String toString() {
        StringBuilder sb = new StringBuilder();
        sb.append(major).append('.').append(minor).append('.').append(patch);
        if (preReleases != null) {
            for (String preRelease : preReleases)
                sb.append('-').append(preRelease);
        }
        if (build != null)
            sb.append('+').append(build);
        return sb.toString();
    }
}
