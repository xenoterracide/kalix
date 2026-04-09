// SPDX-FileCopyrightText: Copyright © 2026 Caleb Cushing
//
// SPDX-License-Identifier: LicenseRef-AllRightsReserved

package com.xenoterracide.kalyx.resolver;

import java.io.Serializable;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;
import java.util.Locale;
import java.util.Objects;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import org.jspecify.annotations.Nullable;

/**
 * Compares Maven version strings following Maven's version comparison rules.
 *
 * <p>Maven versions are compared by splitting them into tokens.
 *
 * <p>Special qualifiers in order: alpha less than beta less than milestone
 *   less than rc less than snapshot less than release less than sp.
 *
 * @see <a href="https://maven.apache.org/pom.html#Version_Order_Specification">Maven Version Order Spec</a>
 */
public final class VersionComparator implements Comparator<String>, Serializable {

  private static final long serialVersionUID = 1L;

  private static final Pattern TOKEN_PATTERN = Pattern.compile("[\\.-]?");

  private static final int RANK_ALPHA = 1;
  private static final int RANK_BETA = 2;
  private static final int RANK_MILESTONE = 3;
  private static final int RANK_RC = 4;
  private static final int RANK_SNAPSHOT = 5;
  private static final int RANK_RELEASE = 6;
  private static final int RANK_SP = 7;

  private VersionComparator() {
    // singleton
  }

  /**
   * Singleton instance.
   */
  public static final VersionComparator INSTANCE = new VersionComparator();

  @Override
  public int compare(String v1, String v2) {
    Objects.requireNonNull(v1, "v1");
    Objects.requireNonNull(v2, "v2");

    List<String> tokens1 = splitTokens(v1);
    List<String> tokens2 = splitTokens(v2);

    int len = Math.max(tokens1.size(), tokens2.size());
    for (int i = 0; i < len; i++) {
      String t1 = i < tokens1.size() ? tokens1.get(i) : "";
      String t2 = i < tokens2.size() ? tokens2.get(i) : "";

      int cmp = compareTokens(t1, t2);
      if (cmp != 0) {
        return cmp;
      }
    }
    return 0;
  }

  /**
   * Splits a version string into tokens.
   *
   * @param version the version string to split
   * @return list of tokens
   */
  @SuppressWarnings("Var")
  private static List<String> splitTokens(String version) {
    List<String> tokens = new ArrayList<>();
    Matcher matcher = TOKEN_PATTERN.matcher(version);
    int pos = 0;
    while (pos < version.length()) {
      int next = matcher.find(pos) ? matcher.start() : version.length();
      if (next > pos) {
        tokens.add(version.substring(pos, next));
      }
      if (matcher.hitEnd()) {
        break;
      }
      // Skip the delimiter
      pos = matcher.end();
    }
    // Remove empty tokens
    tokens.removeIf(String::isEmpty);
    return tokens;
  }

  /**
   * Compares two version tokens.
   *
   * @param t1 first token
   * @param t2 second token
   * @return comparison result (negative if t1 less than t2,
   *     positive if t1 greater than t2, zero if equal)
   */
  private static int compareTokens(String t1, String t2) {
    // Handle empty tokens
    int emptyCompare = compareEmpty(t1, t2);
    if (emptyCompare != -2) {
      return emptyCompare;
    }

    // Try numeric comparison first
    Integer n1 = tryParseNumber(t1);
    Integer n2 = tryParseNumber(t2);

    if (n1 != null && n2 != null) {
      return Integer.compare(n1, n2);
    }

    // One or both are non-numeric, use qualifier comparison
    return compareQualifiers(t1, t2);
  }

  /**
   * Compares empty strings.
   *
   * @param s1 first string
   * @param s2 second string
   * @return -2 if not both empty, otherwise comparison result
   */
  @SuppressWarnings("Var") // result is reassigned in conditional branches
  private static int compareEmpty(String s1, String s2) {
    int result = -2;
    if (s1.isEmpty() && s2.isEmpty()) {
      result = 0;
    } else if (s1.isEmpty()) {
      result = -1;
    } else if (s2.isEmpty()) {
      result = 1;
    }
    return result;
  }

  /**
   * Tries to parse a string as a positive integer.
   *
   * @param s the string to parse
   * @return the integer value, or null if not a valid number
   */
  private static @Nullable Integer tryParseNumber(String s) {
    try {
      // Must be all digits and not have leading zeros
      if (s.isEmpty() || (s.length() > 1 && s.charAt(0) == '0')) {
        return null;
      }
      return Integer.parseInt(s);
    } catch (NumberFormatException e) {
      return null;
    }
  }

  /**
   * Compares version qualifiers.
   *
   * @param q1 first qualifier
   * @param q2 second qualifier
   * @return comparison result
   */
  private static int compareQualifiers(String q1, String q2) {
    int rank1 = getQualifierRank(q1);
    int rank2 = getQualifierRank(q2);

    if (rank1 != rank2) {
      return Integer.compare(rank1, rank2);
    }

    // Same rank, compare lexicographically
    return q1.compareToIgnoreCase(q2);
  }

  /**
   * Gets the sort rank for a qualifier string.
   *
   * @param q the qualifier string
   * @return the sort rank
   */
  private static int getQualifierRank(String q) {
    String lower = q.toLowerCase(Locale.ROOT);
    return switch (lower) {
      case "alpha", "a" -> RANK_ALPHA;
      case "beta", "b" -> RANK_BETA;
      case "milestone", "m" -> RANK_MILESTONE;
      case "rc", "cr" -> RANK_RC;
      case "snapshot" -> RANK_SNAPSHOT;
      case "" -> RANK_RELEASE;
      case "sp" -> RANK_SP;
      default -> getDefaultRank(q);
    };
  }

  /**
   * Gets default rank for unknown qualifiers.
   *
   * @param q the qualifier string
   * @return default rank
   */
  private static int getDefaultRank(String q) {
    // Unknown qualifiers sort like rc
    if (Character.isDigit(q.charAt(0))) {
      return 0;
    }
    return RANK_RC;
  }
}
