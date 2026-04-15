

**BODS XML PRE-FLIGHT CHECKER**

Software Requirements Specification

*Version 1.0  |  Java Diagnostic Utility*

| Project | BODS XML Pre-Flight Checker |
| :---- | :---- |
| **Purpose** | Diagnose XML/XSD compatibility issues before BODS load\_to\_xml execution |
| **Language** | Java (JDK 11+) |
| **Target Audience** | AI Coding Agent / Java Developer |

# **1\. Project Overview**

SAP BusinessObjects Data Services (BODS) provides the load\_to\_xml() function to convert a Nested Relational Data Model (NRDM) table into XML. This function fails silently — returning NULL for scalar columns and empty tables for NRDM columns — with no actionable error messages.

The BODS XML Pre-Flight Checker is a standalone Java command-line utility that diagnoses all known causes of load\_to\_xml() failure before any data is passed to BODS. It analyzes the XML file, the XSD schema file, their compatibility, and produces BODS-specific parameter recommendations.

## **1.1 Problem Statement**

When load\_to\_xml() fails in BODS, the following root causes are possible but not surfaced by the tool:

* NRDM structure does not match the XSD/DTD definition

* is\_top\_level\_element parameter set incorrectly (0 vs 1\)

* max\_size parameter too small, causing silent truncation

* NULL values in required fields causing entire row to return NULL

* Encoding mismatch between XML header and target datastore

* XML elements present in data but absent from schema (or vice versa)

## **1.2 Solution Summary**

A Java utility that accepts an XML file and an XSD file as input, performs deep diagnostic analysis across four dimensions, and outputs a human-readable report with exact BODS parameter recommendations.

# **2\. Scope and Boundaries**

## **2.1 In Scope**

* XSD file structural analysis (root elements, hierarchy, cardinality, data types, required vs optional fields)

* XML file analysis (root element, size, encoding, NULL/empty elements, hierarchy)

* Cross-validation between XML and XSD

* BODS parameter recommendations (is\_top\_level\_element, max\_size, replace\_null\_string, xml\_header)

* Detailed diagnostic report in plain text and optionally HTML

* Command-line interface (CLI) execution

* Exit codes suitable for CI/CD or scripted pipelines

## **2.2 Out of Scope**

* Actual BODS connectivity or execution

* DTD-based validation (XSD only in v1.0)

* GUI or web interface

* Database connectivity

* Transformation or fixing of the XML/XSD — diagnosis only

# **3\. Functional Requirements**

## **3.1 Module 1 — XSD Schema Analyzer**

This module parses and analyzes the XSD file independently of the XML file.

### **3.1.1 Root Element Identification**

* Parse the XSD and identify the root element name and namespace

* Determine whether the root element has exactly one unbounded child element

* If yes: capture the child element name and its maxOccurs value

* Output the recommended is\_top\_level\_element value based on this logic:

  * Root element matches NRDM table name → is\_top\_level\_element \= 1

  * Root element wraps a single unbounded child → is\_top\_level\_element \= 0

### **3.1.2 Element Inventory**

* Enumerate all elements defined in the XSD with their full XPath

* For each element record: name, data type (xs:string, xs:integer, xs:date, etc.), minOccurs, maxOccurs, nillable flag

* Identify all elements where minOccurs \>= 1 and nillable \= false — these are REQUIRED fields

* Identify all elements where minOccurs \= 0 — these are OPTIONAL fields (safe for NULL)

* Flag any xs:anyType or untyped elements as warnings

### **3.1.3 Hierarchy Depth Analysis**

* Measure maximum nesting depth of elements in the XSD

* Identify complex type elements (elements containing child elements)

* Map the complete parent-child relationship tree

### **3.1.4 Schema Statistics**

* Total number of element definitions

* Total required elements

* Total optional elements

* Maximum nesting depth

* Namespace declarations

## **3.2 Module 2 — XML File Analyzer**

This module parses and analyzes the XML file independently of the XSD.

### **3.2.1 File-Level Analysis**

* Read and report the exact file size in bytes and kilobytes

* Calculate and report the character count of the entire XML content

* Recommend max\_size value as: character count \* 1.2 (20% buffer), rounded up to nearest 1000

* If character count exceeds 4000 (BODS default), flag this prominently as a warning

* Detect encoding from the XML declaration (e.g., UTF-8, ISO-8859-1)

* Report the full xml\_header string exactly as it appears in the XML declaration

* Flag if encoding is non-UTF-8 as this requires explicit xml\_header parameter in BODS

### **3.2.2 Root Element Analysis**

* Identify the root element name of the XML document

* Report the root element namespace if present

* Count direct children of the root element

* Report whether root has a single repeating child type or multiple child types

### **3.2.3 NULL and Empty Element Detection**

* Traverse every element in the XML document

* Classify each element into one of: has text content, has child elements, is empty (no content no children), has xsi:nil='true' attribute

* For each NULL or empty element: record the full XPath location, parent element, element name

* Aggregate: total NULL elements, total empty elements, list of all affected XPaths

* This is the most critical output — NULL in a required XSD field causes BODS to return NULL for the entire row

### **3.2.4 XML Element Inventory**

* Enumerate all distinct element names that appear in the XML

* Record occurrence count for each element name

* Record the maximum nesting depth reached in the actual XML data

## **3.3 Module 3 — Cross-Validation Engine**

This module compares the XML against the XSD and identifies all mismatches.

### **3.3.1 Root Element Match Check**

* Compare the XML root element name to the XSD root element name

* If they match: report PASS and recommend is\_top\_level\_element \= 1

* If they do not match: check if the XML root matches the single unbounded child of the XSD root

* If child match: report PASS with note and recommend is\_top\_level\_element \= 0

* If neither matches: report FAIL with both names shown clearly

### **3.3.2 Required Field NULL Check**

* For every element identified as required in XSD (minOccurs \>= 1, nillable \= false):

  * Check whether that element appears as NULL or empty in the XML

  * If yes: report as CRITICAL — this field will cause row-level NULL in BODS

* Report a consolidated list of all required fields that are NULL in the XML

### **3.3.3 Element Presence Check**

* Identify elements present in the XML but NOT defined in the XSD — report as WARNING

* Identify elements defined in the XSD as required but ABSENT from the XML — report as ERROR

* Identify elements defined in the XSD as optional and absent from the XML — report as INFO

### **3.3.4 Data Type Validation**

* For each leaf element in XML that maps to a typed XSD element:

  * Validate xs:integer — check value is parseable as integer

  * Validate xs:date — check value matches yyyy-MM-dd pattern

  * Validate xs:dateTime — check ISO 8601 format

  * Validate xs:decimal — check value is parseable as decimal

  * Validate xs:boolean — check value is true/false/1/0

* Report all type mismatches with XPath, actual value, and expected type

### **3.3.5 Namespace Consistency Check**

* Compare namespace declarations between XML and XSD

* Flag any namespace mismatch as WARNING — BODS validation may reject mismatched namespaces

## **3.4 Module 4 — BODS Parameter Recommendation Engine**

This module synthesizes all analysis results into actionable BODS load\_to\_xml() parameter recommendations.

### **3.4.1 Parameter: is\_top\_level\_element**

* Based on root element match analysis from 3.3.1, output a definitive recommendation: 0 or 1

* Include explanation of why this value was chosen

* If determination cannot be made confidently, report AMBIGUOUS with both scenarios explained

### **3.4.2 Parameter: max\_size**

* Report actual XML character length

* Report recommended max\_size \= ceil(character\_count \* 1.2 / 1000\) \* 1000

* If actual XML is a sample/subset, note that production max\_size should be sized for the largest expected document

* Flag if calculated max\_size exceeds BODS default of 4000 — user must explicitly set this parameter

### **3.4.3 Parameter: replace\_null\_string**

* If any NULL or empty elements were found: recommend setting replace\_null\_string to a value such as empty string or a sentinel

* List every XPath where a NULL replacement would apply

* If no NULLs found: recommend NULL (pass-through behavior)

### **3.4.4 Parameter: xml\_header**

* If encoding is UTF-8 and XML declaration is standard: recommend NULL (use BODS default)

* If encoding is non-UTF-8: output the exact xml\_header string the user should pass to BODS

* If XML has no declaration: recommend passing an explicit header

### **3.4.5 Parameter: enable\_validation**

* If all cross-validation checks pass: recommend enable\_validation \= 1

* If any WARNING or ERROR found: recommend enable\_validation \= 0 until issues resolved, list the issues

# **4\. Output Report Specification**

## **4.1 Report Structure**

The report must be output to the console (stdout) by default, with an optional flag to write to a .txt or .html file. The report must be structured in the following sections in order:

1. Report Header — tool name, version, timestamp, files analyzed

2. SECTION A: XSD Analysis Summary

3. SECTION B: XML Analysis Summary

4. SECTION C: Cross-Validation Results

5. SECTION D: BODS Parameter Recommendations

6. SECTION E: Issues Summary (CRITICAL / ERROR / WARNING / INFO counts)

7. SECTION F: Overall Pre-Flight Status — PASS, PASS WITH WARNINGS, or FAIL

## **4.2 Issue Severity Levels**

| Severity | Meaning | BODS Impact |
| :---- | :---- | :---- |
| CRITICAL | Will definitely cause load\_to\_xml() to return NULL | Row-level failure, silent data loss |
| ERROR | Will likely cause load\_to\_xml() to fail or produce wrong output | Possible NULL or malformed XML |
| WARNING | May cause issues depending on BODS configuration | Unpredictable behavior |
| INFO | Informational — no failure expected but worth noting | No impact expected |
| PASS | Check completed with no issues found | No action needed |

## **4.3 Sample Report Structure**

\================================================================

  BODS XML PRE-FLIGHT CHECKER v1.0

  Timestamp : 2024-11-15 09:32:11

  XML File  : /data/billing\_address.xml

  XSD File  : /schema/billing\_address.xsd

\================================================================

SECTION A: XSD ANALYSIS

  Root Element        : allBillingAddresses

  Single Child Element: billingAddress (maxOccurs=unbounded)

  Total Elements      : 24

  Required Elements   : 8

  Optional Elements   : 16

  Max Nesting Depth   : 4

SECTION B: XML ANALYSIS

  Root Element        : billingAddress

  File Size           : 3,412 bytes (3.3 KB)

  Character Count     : 3,398 characters

  Encoding            : UTF-8

  NULL Elements Found : 2

    \[CRITICAL\] /billingAddress/postalCode  (required in XSD)

    \[INFO\]     /billingAddress/addressLine2 (optional in XSD)

SECTION C: CROSS-VALIDATION

  Root Match          : PASS (child match — is\_top\_level\_element \= 0\)

  Required Fields     : 1 CRITICAL issue found

  Unknown Elements    : PASS

  Data Types          : PASS

SECTION D: BODS PARAMETER RECOMMENDATIONS

  nested\_table\_name   : billingAddress

  schema\_dtd\_name     : billing\_address.xsd

  is\_top\_level\_element: 0

  max\_size            : 5000  (actual: 3398, buffered: 4078\)

  xml\_header          : NULL  (UTF-8 default is acceptable)

  replace\_null\_string : ''    (empty string — 2 NULLs found)

  enable\_validation   : 0     (resolve CRITICAL before enabling)

SECTION E: ISSUES SUMMARY

  CRITICAL : 1

  ERROR    : 0

  WARNING  : 0

  INFO     : 1

SECTION F: PRE-FLIGHT STATUS

  \*\*\* FAIL — Resolve 1 CRITICAL issue before running in BODS \*\*\*

\================================================================

# **5\. Command-Line Interface Specification**

## **5.1 Invocation Syntax**

java \-jar bods-preflight.jar \--xml \<path\> \--xsd \<path\> \[OPTIONS\]

## **5.2 Required Arguments**

| Argument | Description |
| :---- | :---- |
| \--xml \<file\_path\> | Path to the XML file to be analyzed |
| \--xsd \<file\_path\> | Path to the XSD schema file to be analyzed |

## **5.3 Optional Arguments**

| Argument | Default | Description |
| :---- | :---- | :---- |
| \--output \<file\_path\> | stdout | Write report to a file instead of console |
| \--format \<txt|html\> | txt | Report output format |
| \--verbose | off | Include element-level detail in report |
| \--fail-on-warning | off | Exit with code 2 if any WARNING found (stricter mode) |
| \--encoding \<charset\> | auto-detect | Force a specific encoding for XML parsing |
| \--help | n/a | Print usage information |

## **5.4 Exit Codes**

| Code | Meaning |
| :---- | :---- |
| 0 | Pre-flight PASS — no CRITICAL or ERROR issues found |
| 1 | Pre-flight FAIL — one or more CRITICAL or ERROR issues found |
| 2 | Pre-flight PASS WITH WARNINGS (only if \--fail-on-warning is set) |
| 3 | Tool execution error — invalid arguments, file not found, parse error |

# **6\. Technical Requirements**

## **6.1 Language and Runtime**

* Language: Java

* Minimum JDK version: JDK 11

* Build tool: Maven or Gradle (developer choice)

* Deliverable: a single executable JAR file (bods-preflight.jar) with no external runtime dependencies

## **6.2 Allowed Java Standard Libraries**

Use only Java standard library packages — no third-party dependencies required:

* javax.xml.parsers — DOM parsing for XML and XSD

* javax.xml.validation — XSD schema validation

* javax.xml.xpath — XPath expression evaluation

* org.w3c.dom — DOM tree traversal

* java.io, java.nio.file — file reading and writing

* java.nio.charset — encoding detection

* java.util — collections, formatting

*NOTE: Do NOT use JAXB, SAX (unless performance requires it), or any external library like Apache Xerces explicitly — the JDK bundles its own implementation.*

## **6.3 Architecture**

* Implement each module (3.1 through 3.4) as a separate Java class

* Use a main orchestrator class (BodsPreflightChecker.java) to coordinate modules

* Use a shared DiagnosticReport object to accumulate issues across modules

* Use an Issue class with fields: severity (enum), message, xPath, recommendation

* ReportWriter class responsible for rendering the DiagnosticReport to text or HTML

## **6.4 Suggested Class Structure**

| Class | Responsibility |
| :---- | :---- |
| BodsPreflightChecker | Main entry point, argument parsing, orchestration |
| XsdAnalyzer | Implements Module 3.1 — parses and analyzes XSD |
| XmlAnalyzer | Implements Module 3.2 — parses and analyzes XML |
| CrossValidator | Implements Module 3.3 — compares XML vs XSD |
| ParameterRecommender | Implements Module 3.4 — generates BODS recommendations |
| DiagnosticReport | Data holder for all issues and analysis results |
| Issue | Represents a single diagnostic finding with severity and detail |
| ReportWriter | Renders the DiagnosticReport to stdout, .txt, or .html |
| Severity (enum) | CRITICAL, ERROR, WARNING, INFO, PASS |

## **6.5 Error Handling**

* If the XML file cannot be found: print clear error, exit code 3

* If the XSD file cannot be found: print clear error, exit code 3

* If the XML file is not well-formed XML: print parse error with line number, exit code 3

* If the XSD file is not a valid schema: print schema error with detail, exit code 3

* All unexpected exceptions must be caught and reported with a friendly message — never print a raw Java stack trace to the user

# **7\. Non-Functional Requirements**

## **7.1 Performance**

* Must process XML files up to 50 MB within 30 seconds on standard hardware

* Memory usage must not exceed 512 MB for files up to 50 MB

## **7.2 Usability**

* All output messages must be in plain English — no Java exception class names visible to user

* Report must be readable without any XML or Java knowledge

* BODS parameter recommendations must be copy-pasteable directly into BODS

## **7.3 Portability**

* Must run on Windows, Linux, and macOS with JDK 11+

* No OS-specific paths or line endings in output

## **7.4 Maintainability**

* Each module must be independently testable

* Code must include inline JavaDoc comments for all public methods

* Severity thresholds and buffer percentages (e.g., 1.2x for max\_size) must be configurable constants, not hardcoded magic numbers

# **8\. Test Requirements**

## **8.1 Unit Tests**

Each module must have accompanying unit tests covering the following scenarios:

### **8.1.1 XsdAnalyzer Tests**

* XSD with root element and no children — is\_top\_level\_element \= 1

* XSD with root wrapping single unbounded child — is\_top\_level\_element \= 0

* XSD with multiple root children — AMBIGUOUS warning

* XSD with required elements (minOccurs=1, nillable=false)

* XSD with optional elements (minOccurs=0)

### **8.1.2 XmlAnalyzer Tests**

* XML with UTF-8 encoding — xml\_header recommendation is NULL

* XML with ISO-8859-1 encoding — xml\_header recommendation is explicit string

* XML character count under 4000 — no max\_size warning

* XML character count over 4000 — max\_size warning and buffered recommendation

* XML with xsi:nil=true elements — flagged as NULL

* XML with empty self-closing elements — flagged as empty

### **8.1.3 CrossValidator Tests**

* XML root matches XSD root — is\_top\_level\_element \= 1

* XML root matches XSD child — is\_top\_level\_element \= 0

* XML root matches neither — FAIL

* Required XSD field is NULL in XML — CRITICAL

* Optional XSD field is NULL in XML — INFO

* Element in XML not in XSD — WARNING

* Required element missing from XML entirely — ERROR

### **8.1.4 End-to-End Tests**

* Provide a sample XML \+ XSD pair that fully passes — expect exit code 0

* Provide a sample XML \+ XSD pair with a required-field NULL — expect exit code 1

* Provide a sample XML \+ XSD with root mismatch — expect exit code 1

* Provide XML with character count \> 4000 — expect max\_size \> 4000 recommendation

# **9\. Deliverables**

| Deliverable | Description |
| :---- | :---- |
| bods-preflight.jar | Executable JAR file runnable with java \-jar |
| Source code | All Java source files with JavaDoc comments |
| Unit tests | JUnit test classes for each module |
| README.md | Installation, usage examples, and argument reference |
| sample-pass/ | Folder with sample XML+XSD that produces a PASS result |
| sample-fail/ | Folder with sample XML+XSD that produces a FAIL result |

# **10\. Glossary**

| Term | Definition |
| :---- | :---- |
| BODS | SAP BusinessObjects Data Services — ETL/data integration platform |
| NRDM | Nested Relational Data Model — BODS internal structure for hierarchical data |
| load\_to\_xml() | BODS function that converts NRDM table to XML string |
| XSD | XML Schema Definition — defines the structure and data types of an XML document |
| DTD | Document Type Definition — older alternative to XSD (out of scope for v1.0) |
| is\_top\_level\_element | BODS parameter: 1 if NRDM root matches XSD root; 0 if it matches XSD root's child |
| max\_size | BODS parameter: maximum character length of generated XML output |
| replace\_null\_string | BODS parameter: string to substitute for NULL values in XML output |
| xml\_header | BODS parameter: XML declaration override; NULL uses BODS default UTF-8 header |
| enable\_validation | BODS parameter: 1 validates generated XML against XSD; 0 skips validation |
| minOccurs | XSD attribute: minimum number of times an element must appear (0 \= optional) |
| nillable | XSD attribute: if true, element can carry xsi:nil=true to represent NULL |
| CRITICAL | Severity level indicating the issue will definitively cause BODS row-level failure |

***End of Requirements Document***

BODS XML Pre-Flight Checker — SRS v1.0