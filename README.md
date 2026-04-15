# BODS XML Pre-Flight Checker

Java command-line utility for diagnosing XML/XSD compatibility issues before SAP BODS `load_to_xml()` runs.

---

## Requirements Source of Truth

`C:\Users\achan\Downloads\BODS_XML_PreFlight_Requirements.docx`

---

## Project Layout

```
src/main/java/          Java source code
src/test/java/          JUnit 5 tests
src/test/resources/     Test fixtures (XSD, XML)
fixtures/pairs/         End-to-end fixture pairs used by integration tests and manual runs
pom.xml                 Maven build descriptor
```

---

## Deploying into Eclipse on Another Computer

### Prerequisites

- **Java 11 or higher** installed (check with `java -version`)
- **Eclipse IDE for Java Developers** (2020-06 or newer recommended)
- The **m2e** Maven integration plug-in — bundled with Eclipse IDE for Java Developers by default

No Maven installation is required on the target machine. Eclipse's embedded Maven handles everything.

---

### Step 1 — Transfer the Project

Copy the entire project folder to the target computer. The folder must contain `pom.xml` at its root. All of the following must be present:

```
pom.xml
src/
fixtures/
README.md
```

Common transfer methods:

- Copy via USB drive or shared network folder
- Clone from Git if the project is in a repository (`git clone <url>`)
- Zip the folder, transfer, and unzip

---

### Step 2 — Import into Eclipse

1. Open Eclipse.
2. Go to **File → Import…**
3. Expand **Maven** and select **Existing Maven Projects**, then click **Next**.
4. Click **Browse…** and navigate to the folder that contains `pom.xml`.
5. Eclipse will detect `/pom.xml` and show it checked in the list.
6. Click **Finish**.

Eclipse will import the project and download all dependencies (JUnit 5) from Maven Central on first import. This requires an internet connection the first time.

---

### Step 3 — Verify the Build

Once the import completes and the dependency download bar disappears:

1. Right-click the project in **Package Explorer**.
2. Choose **Run As → Maven build…**
3. In the **Goals** field type: `test`
4. Click **Run**.

The Console view will show:

```
Tests run: 50, Failures: 0, Errors: 0, Skipped: 0
BUILD SUCCESS
```

If any test fails, the Console shows the test name and the assertion message.

---

### Step 4 — Build the Executable JAR

1. Right-click the project → **Run As → Maven build…**
2. Goals: `package -DskipTests`
3. Click **Run**.

The JAR is produced at:

```
target/bods-preflight-1.0.0-SNAPSHOT.jar
```

---

### Step 5 — Run from Eclipse Directly (Optional)

To run the checker inside Eclipse without building a JAR:

1. Open `src/main/java/com/bods/preflight/BodsPreflightChecker.java`.
2. Right-click in the editor → **Run As → Run Configurations…**
3. Double-click **Java Application** to create a new configuration.
4. Set **Main class** to `com.bods.preflight.BodsPreflightChecker`.
5. Switch to the **Arguments** tab and enter the program arguments, for example:

   ```
   --xml fixtures/pairs/pass/sample.xml --xsd fixtures/pairs/pass/sample.xsd
   ```

6. Set the **Working directory** to `${workspace_loc:bods-preflight}` (the project root).
7. Click **Run**.

Output appears in the Console view.

---

## Manual Testing

All commands below assume the JAR has been built (`mvn package` or the Eclipse step above) and the terminal is open in the project root folder.

### Build the JAR first

```bash
mvn package -DskipTests
```

This produces `target/bods-preflight-1.0.0-SNAPSHOT.jar`.

---

### Test 1 — Clean Pass (exit code 0)

**Fixture:** `fixtures/pairs/pass/`
XML root `billingAddress` matches the single unbounded child of the XSD wrapper. All required fields are populated. No issues expected.

```bash
java -jar target/bods-preflight-1.0.0-SNAPSHOT.jar \
  --xml fixtures/pairs/pass/sample.xml \
  --xsd fixtures/pairs/pass/sample.xsd
```

**Expected:**
```
SECTION F: PRE-FLIGHT STATUS
  *** PASS - No blocking issues found ***
```
Exit code: **0**

---

### Test 2 — Required Field Is Null (exit code 1)

**Fixture:** `fixtures/pairs/fail-required-null/`
The XML contains `xsi:nil="true"` on a required field (`postalCode`). BODS will output NULL for that row.

```bash
java -jar target/bods-preflight-1.0.0-SNAPSHOT.jar \
  --xml fixtures/pairs/fail-required-null/sample.xml \
  --xsd fixtures/pairs/fail-required-null/sample.xsd
```

**Expected:**
```
SECTION F: PRE-FLIGHT STATUS
  *** FAIL - Resolve critical or error issues before running in BODS ***
```
Exit code: **1**

---

### Test 3 — XML Root Does Not Match XSD (exit code 1)

**Fixture:** `fixtures/pairs/fail-root-mismatch/`
The XML root element is `customerAddress` but the XSD defines `allBillingAddresses` (wrapper) with child `billingAddress`. Neither root nor child matches, so BODS would fail to load.

```bash
java -jar target/bods-preflight-1.0.0-SNAPSHOT.jar \
  --xml fixtures/pairs/fail-root-mismatch/sample.xml \
  --xsd fixtures/pairs/fail-root-mismatch/sample.xsd
```

**Expected:**
```
Root Match  : FAIL (XML root 'customerAddress' does not match ...)
SECTION F: PRE-FLIGHT STATUS
  *** FAIL - Resolve critical or error issues before running in BODS ***
```
Exit code: **1**

---

### Test 4 — Large XML Triggers max_size Warning (exit code 0)

**Fixture:** `fixtures/pairs/warn-large-xml/`
The XML is over 4000 characters, which exceeds the BODS default `max_size`. The tool warns and recommends a higher value. Without `--fail-on-warning` the run still passes.

```bash
java -jar target/bods-preflight-1.0.0-SNAPSHOT.jar \
  --xml fixtures/pairs/warn-large-xml/sample.xml \
  --xsd fixtures/pairs/warn-large-xml/sample.xsd
```

**Expected:**
```
[WARNING] ... XML character count exceeds the BODS default max_size of 4000 ...
max_size : 6000  (actual: 4687, buffered ...)
SECTION F: PRE-FLIGHT STATUS
  *** PASS - No blocking issues found ***
```
Exit code: **0**

---

### Test 4b — Promote Warning to Failure with `--fail-on-warning` (exit code 2)

Same fixture, but adding `--fail-on-warning` causes exit code 2 when any WARNING is present.

```bash
java -jar target/bods-preflight-1.0.0-SNAPSHOT.jar \
  --xml fixtures/pairs/warn-large-xml/sample.xml \
  --xsd fixtures/pairs/warn-large-xml/sample.xsd \
  --fail-on-warning
```

Exit code: **2**

---

### Test 5 — Unknown XML Element Warning (exit code 0)

**Fixture:** `fixtures/pairs/warn-unknown-element/`
The XML contains a field not defined in the XSD. BODS will silently ignore it, but it is flagged as a WARNING so you know the schema is out of sync.

```bash
java -jar target/bods-preflight-1.0.0-SNAPSHOT.jar \
  --xml fixtures/pairs/warn-unknown-element/sample.xml \
  --xsd fixtures/pairs/warn-unknown-element/sample.xsd
```

**Expected:**
```
[WARNING] ... XML element is present in data but not defined in the XSD ...
```
Exit code: **0**

---

### Test 6 — Save Report to a File

Add `--output` to write the report to a file instead of (or in addition to) printing to the console.

```bash
java -jar target/bods-preflight-1.0.0-SNAPSHOT.jar \
  --xml fixtures/pairs/pass/sample.xml \
  --xsd fixtures/pairs/pass/sample.xsd \
  --output report.txt
```

Open `report.txt` to review the full report.

---

### Test 7 — HTML Report

```bash
java -jar target/bods-preflight-1.0.0-SNAPSHOT.jar \
  --xml fixtures/pairs/pass/sample.xml \
  --xsd fixtures/pairs/pass/sample.xsd \
  --format html \
  --output report.html
```

Open `report.html` in a browser to view the formatted report.

---

### CLI Reference

| Option | Description |
|---|---|
| `--xml <path>` | Path to the XML file to analyze |
| `--xsd <path>` | Path to the XSD schema file |
| `--output <path>` | Write report to this file (default: console only) |
| `--format text\|html` | Report format (default: text) |
| `--fail-on-warning` | Exit with code 2 if any WARNING is present |
| `--encoding <charset>` | Override the XML encoding (e.g. `ISO-8859-1`) |

### Exit Codes

| Code | Meaning |
|---|---|
| 0 | PASS — no blocking issues |
| 1 | FAIL — one or more CRITICAL or ERROR issues found |
| 2 | PASS WITH WARNINGS — warnings present and `--fail-on-warning` was set |
| 3 | Execution error — file not found, parse failure, etc. |

---

## Running the Automated Tests

### From the terminal

```bash
mvn test
```

### From Eclipse

Right-click the project → **Run As → Maven build…** → Goals: `test` → Run.

Or right-click any test class (e.g. `EndToEndTest`) → **Run As → JUnit Test**.

### Expected result

```
Tests run: 50, Failures: 0, Errors: 0, Skipped: 0
BUILD SUCCESS
```

---

## Testing Your Own XML and XSD Files

You can point the tool at any XML/XSD pair without modifying the project.

### Basic run

```bash
java -jar target/bods-preflight-1.0.0-SNAPSHOT.jar \
  --xml path/to/your/file.xml \
  --xsd path/to/your/schema.xsd
```

Paths can be absolute or relative to the folder where you run the command.

### Save the report to a file

```bash
java -jar target/bods-preflight-1.0.0-SNAPSHOT.jar \
  --xml path/to/your/file.xml \
  --xsd path/to/your/schema.xsd \
  --output report.txt
```

### Get an HTML report

```bash
java -jar target/bods-preflight-1.0.0-SNAPSHOT.jar \
  --xml path/to/your/file.xml \
  --xsd path/to/your/schema.xsd \
  --format html \
  --output report.html
```

Open `report.html` in a browser.

### Running from Eclipse (no JAR needed)

If you are on the target machine and have not built the JAR yet, use a Run Configuration as described in the **Deploying into Eclipse** section above. Paste your file paths into the **Arguments** tab and click Run.

### Making a pair a permanent fixture

If you want the new files tested automatically every time `mvn test` runs:

1. Create a new folder under `fixtures/pairs/`, for example `fixtures/pairs/my-scenario/`.
2. Place your files there as `sample.xml` and `sample.xsd`.
3. Add a test method to [EndToEndTest.java](src/test/java/com/bods/preflight/integration/EndToEndTest.java) following the same pattern as the existing tests.
