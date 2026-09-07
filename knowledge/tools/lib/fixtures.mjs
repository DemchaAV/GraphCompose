/**
 * knowledge/tools/lib/fixtures.mjs — the smallest test harness that says what
 * failed.
 *
 * The knowledge tools ship no dependencies, so their fixture suites are plain
 * node scripts. Each one had grown its own byte-identical copy of this: four
 * copies of ten lines, which is four places to improve the reporting and four
 * chances for one of them to drift into saying something slightly different
 * about a failure.
 *
 * Deliberately tiny. `node --test` exists, but these suites are run one file at
 * a time by CI and by hand, and their whole value is a diff of expected against
 * actual — a runner would add ceremony without adding that.
 */

/**
 * A suite of checks that reports once, at the end, and sets the exit code.
 *
 * @param {string} name shown in the summary line, e.g. `anchors.test`
 * @returns {{check: (name: string, actual: unknown, expected: unknown) => void, fail: (message: string) => void, done: () => never}}
 */
export function suite(name) {
  let failures = 0;
  let passes = 0;

  return {
    /** Compare by value; anything JSON can express is fair game. */
    check(what, actual, expected) {
      const a = JSON.stringify(actual);
      const e = JSON.stringify(expected);
      if (a === e) {
        passes += 1;
        return;
      }
      failures += 1;
      process.stdout.write(`  FAIL  ${what}\n        expected ${e}\n        actual   ${a}\n`);
    },

    /** A failure with no comparison behind it — a missing file, a bad shape. */
    fail(message) {
      failures += 1;
      process.stdout.write(`  FAIL  ${message}\n`);
    },

    /** Print the summary and exit; never returns. */
    done() {
      process.stdout.write(
        failures ? `\n[${name}] ${failures} failed, ${passes} passed\n` : `[${name}] ${passes} passed\n`,
      );
      process.exit(failures ? 1 : 0);
    },
  };
}
