# BODS XML Pre-Flight Checker Implementation Helper

This file is the implementation guide for building the Java utility described in:

`C:\Users\achan\Downloads\BODS_XML_PreFlight_Requirements.docx`

This helper is intentionally written so the code can be implemented step by step from this document alone, while treating the Word document as the authoritative requirements source.

## 1. Purpose of This Helper

Use this file as the execution plan for implementation. It translates the requirements document into:

- a recommended project structure
- a phased build order
- class-by-class responsibilities
- data model definitions
- processing rules
- report rendering rules
- test scenarios
- implementation cautions and assumptions

This is a planning document only. It does not change the requirements. If this helper conflicts with the Word document, the Word document wins.

## 2. Primary Goal

Build a standalone Java CLI utility that analyzes:

- one XML file
- one XSD file

and produces a structured diagnostic report that helps users avoid silent failures in BODS `load_to_xml()`.

The tool must:

- diagnose XML/XSD compatibility issues
- identify likely BODS failure causes
- recommend BODS parameters
- emit a clear human-readable report
- return machine-usable exit codes

## 3. Recommended Implementation Strategy

Do not try to build everything at once.

Build in this order:

1. Project skeleton and CLI argument parsing
2. Shared domain models
3. XSD analyzer
4. XML analyzer
5. Cross-validator
6. Parameter recommender
7. Plain-text report writer
8. Exit code logic
9. Unit tests
10. Sample input files
11. Optional HTML writer

Reason:

- the report and orchestration depend on shared models
- recommendations depend on analysis results
- clean output depends on a stable report model
- tests are easier once the internal contracts are explicit

## 4. Suggested Project Layout

Use Maven unless there is a strong reason to choose Gradle.

Recommended layout:

```text
xml/
  pom.xml
  README.md
  sample-pass/
    sample.xml
    sample.xsd
  sample-fail/
    sample.xml
    sample.xsd
  src/
    main/
      java/
        com/example/bods/preflight/
          BodsPreflightChecker.java
          cli/
            CommandLineOptions.java
            CommandLineParser.java
          model/
            Severity.java
            Issue.java
            DiagnosticReport.java
            AnalysisSummary.java
            XsdAnalysisResult.java
            XmlAnalysisResult.java
            ValidationResult.java
            ParameterRecommendations.java
            ElementDefinition.java
            XmlElementObservation.java
          analyzer/
            XsdAnalyzer.java
            XmlAnalyzer.java
            CrossValidator.java
            ParameterRecommender.java
          report/
            ReportWriter.java
            TextReportWriter.java
            HtmlReportWriter.java
          util/
            XmlUtils.java
            XPathUtils.java
            FormattingUtils.java
            Constants.java
            SafeParsers.java
    test/
      java/
        com/example/bods/preflight/
          analyzer/
            XsdAnalyzerTest.java
            XmlAnalyzerTest.java
            CrossValidatorTest.java
            ParameterRecommenderTest.java
          report/
            TextReportWriterTest.java
          integration/
            EndToEndTest.java
```

Notes:

- Package names can be changed, but keep the separation by concern.
- Keep report rendering separate from analysis logic.
- Keep CLI parsing separate from orchestration.

## 5. Non-Negotiable Design Principles

These rules should guide implementation.

### 5.1 Use the Word Document as Source of Truth

The Word document is authoritative for:

- section order
- terminology
- severity meanings
- parameter names
- exit code semantics
- scope boundaries

### 5.2 Build for Common XSD Cases First

Support these in v1:

- one XSD file
- one top-level schema
- element declarations using common `xs:` types
- nested `complexType` and `sequence`
- `minOccurs`, `maxOccurs`, `nillable`
- namespaces at practical, common depth

Do not expand the initial design around rare schema edge cases unless needed by real input files.

### 5.3 Prefer Clear Diagnostics Over Cleverness

If there is uncertainty, report it clearly rather than hiding it.

Example:

- prefer `AMBIGUOUS recommendation for is_top_level_element`
- do not force a value if the schema structure does not support confidence

### 5.4 Keep All Public Results Structured

Each module should return structured result objects, not only strings.

This makes:

- testing easier
- report generation deterministic
- future HTML output straightforward

## 6. Core Domain Model

Design the data model before implementing analyzers.

### 6.1 Severity

Enum values:

- `CRITICAL`
- `ERROR`
- `WARNING`
- `INFO`
- `PASS`

Add helper methods if useful:

- sort priority
- display label
- whether it contributes to failure

### 6.2 Issue

Fields:

- `Severity severity`
- `String category`
- `String message`
- `String xPath`
- `String recommendation`

Optional useful fields:

- `String section`
- `String elementName`
- `String expected`
- `String actual`

Guideline:

- `message` should read naturally for end users
- `xPath` may be null when not applicable

### 6.3 ElementDefinition

Represents an XSD-defined element.

Fields:

- `String name`
- `String qualifiedName`
- `String namespaceUri`
- `String xPath`
- `String dataType`
- `int minOccurs`
- `String maxOccursRaw`
- `boolean unbounded`
- `boolean nillable`
- `boolean required`
- `boolean complexType`
- `String parentXPath`

### 6.4 XmlElementObservation

Represents an XML element encountered during traversal.

Fields:

- `String name`
- `String qualifiedName`
- `String namespaceUri`
- `String xPath`
- `String parentXPath`
- `String textValue`
- `boolean hasTextContent`
- `boolean hasChildElements`
- `boolean empty`
- `boolean nilAttribute`
- `int depth`

### 6.5 XsdAnalysisResult

Fields:

- `String rootElementName`
- `String rootNamespace`
- `String singleChildElementName`
- `String singleChildMaxOccurs`
- `boolean rootHasSingleUnboundedChild`
- `int totalElements`
- `int totalRequiredElements`
- `int totalOptionalElements`
- `int maxNestingDepth`
- `List<String> namespaceDeclarations`
- `List<ElementDefinition> elements`
- `List<Issue> issues`
- `String topLevelElementRecommendationCandidate`

### 6.6 XmlAnalysisResult

Fields:

- `String rootElementName`
- `String rootNamespace`
- `long fileSizeBytes`
- `double fileSizeKb`
- `int characterCount`
- `String encoding`
- `String xmlDeclaration`
- `int rootDirectChildCount`
- `boolean singleRepeatingChildType`
- `int totalNullElements`
- `int totalEmptyElements`
- `int maxNestingDepth`
- `Map<String, Integer> distinctElementCounts`
- `List<XmlElementObservation> elementObservations`
- `List<Issue> issues`

### 6.7 ValidationResult

Fields:

- `boolean rootMatch`
- `boolean childRootMatch`
- `String rootMatchExplanation`
- `List<Issue> issues`
- `List<String> missingRequiredElements`
- `List<String> missingOptionalElements`
- `List<String> unknownXmlElements`
- `List<String> requiredNullElements`
- `List<String> typeMismatchElements`
- `boolean namespaceMatch`

### 6.8 ParameterRecommendations

Fields:

- `String nestedTableName`
- `String schemaDtdName`
- `String isTopLevelElement`
- `String isTopLevelElementExplanation`
- `int actualCharacterCount`
- `int recommendedMaxSize`
- `String maxSizeExplanation`
- `String replaceNullString`
- `List<String> replaceNullStringAffectedPaths`
- `String xmlHeader`
- `String xmlHeaderExplanation`
- `String enableValidation`
- `String enableValidationExplanation`

### 6.9 DiagnosticReport

This is the top-level aggregate.

Fields:

- `String toolName`
- `String version`
- `LocalDateTime timestamp`
- `Path xmlFile`
- `Path xsdFile`
- `XsdAnalysisResult xsdAnalysis`
- `XmlAnalysisResult xmlAnalysis`
- `ValidationResult validationResult`
- `ParameterRecommendations parameterRecommendations`
- `List<Issue> allIssues`
- `String overallStatus`

Required helper behavior:

- count issues by severity
- derive overall status
- merge issues from all modules

## 7. Constants to Define Up Front

Create a constants class so values are not hardcoded all over the codebase.

Required constants:

- tool name = `BODS XML PRE-FLIGHT CHECKER`
- version = `v1.0`
- default format = `txt`
- default fail on warning = `false`
- default verbose = `false`
- BODS default max size = `4000`
- max size buffer multiplier = `1.2`
- max size rounding unit = `1000`
- pass status labels:
  - `PASS`
  - `PASS WITH WARNINGS`
  - `FAIL`

## 8. CLI Design

Implement CLI parsing early, but keep it simple and predictable.

Required arguments:

- `--xml <file_path>`
- `--xsd <file_path>`

Optional arguments:

- `--output <file_path>`
- `--format <txt|html>`
- `--verbose`
- `--fail-on-warning`
- `--encoding <charset>`
- `--help`

### 8.1 CommandLineOptions Object

Fields:

- `Path xmlPath`
- `Path xsdPath`
- `Path outputPath`
- `String format`
- `boolean verbose`
- `boolean failOnWarning`
- `String forcedEncoding`
- `boolean helpRequested`

### 8.2 Parsing Rules

- Reject missing values for flag/value pairs
- Reject unknown arguments
- Reject invalid format values
- Allow flags in any order
- Print friendly usage on `--help`
- Return execution error for invalid CLI usage

### 8.3 Validation Rules

- XML file must exist
- XSD file must exist
- format must be `txt` or `html`
- encoding, if supplied, must be resolvable as a Java charset

## 9. Orchestration Flow

Implement `BodsPreflightChecker` as the main coordinator.

Processing order:

1. Parse arguments
2. Handle `--help`
3. Validate input paths and options
4. Analyze XSD
5. Analyze XML
6. Cross-validate XML against XSD
7. Generate BODS recommendations
8. Merge results into `DiagnosticReport`
9. Render report
10. Write to stdout or file
11. Exit with correct code

Important:

- each step should fail gracefully
- convert low-level exceptions to user-friendly output
- never expose raw stack traces in normal execution

## 10. XSD Analyzer Detailed Instructions

This is the first heavy logic module.

### 10.1 Input

- XSD file path

### 10.2 Output

- `XsdAnalysisResult`

### 10.3 Parser Approach

Use DOM parsing from the Java standard library.

Implementation notes:

- parse the XSD as XML
- identify `xs:schema`
- collect namespace declarations from the schema root
- locate top-level `xs:element` declarations

### 10.4 Required Responsibilities

#### A. Root Element Identification

- find the main top-level root element
- record its name and namespace
- inspect whether it wraps exactly one repeating child
- determine whether that child has `maxOccurs="unbounded"` or another repeating value
- store a recommendation candidate:
  - `1` if root itself appears to represent the table
  - `0` if root wraps a single repeating child
  - `AMBIGUOUS` if the structure is not decisive

#### B. Element Inventory

Traverse all element definitions and record:

- full XPath-like path within schema structure
- local name
- type
- `minOccurs`
- `maxOccurs`
- `nillable`
- whether required
- whether complex or leaf

Definition rules:

- required means `minOccurs >= 1` and `nillable == false`
- optional means `minOccurs == 0`

Warnings:

- add a warning issue for untyped elements
- add a warning issue for `xs:anyType`

#### C. Hierarchy Analysis

Track:

- parent-child relationships
- nesting depth
- complex nodes vs leaf nodes

#### D. Statistics

Calculate:

- total element definitions
- total required
- total optional
- maximum depth
- namespace declarations

### 10.5 Suggested Internal Helpers

- `parseSchemaDocument(Path)`
- `findSchemaRootElement(Document)`
- `extractTopLevelElements(Element schemaRoot)`
- `traverseElementDefinition(...)`
- `resolveElementType(...)`
- `parseOccurs(String raw, int defaultValue)`
- `isUnbounded(String raw)`

### 10.6 Practical Simplifications for v1

Support first:

- inline complex types
- named types when directly referenced
- `xs:sequence`

Defer unless needed:

- `xs:choice`
- `xs:all`
- imported schemas
- circular type references

If unsupported schema constructs are found, emit `WARNING` rather than failing silently.

## 11. XML Analyzer Detailed Instructions

### 11.1 Input

- XML file path
- optional forced encoding

### 11.2 Output

- `XmlAnalysisResult`

### 11.3 File-Level Analysis

Collect:

- exact file size in bytes
- file size in KB
- full character count
- encoding from XML declaration if available
- exact XML declaration string

Implementation guidance:

- read raw bytes first
- preserve the opening declaration if present
- if encoding is forced, use that for parsing
- otherwise detect from declaration, else default safely

### 11.4 max_size Calculation Rule

Use:

`ceil(character_count * 1.2 / 1000) * 1000`

Also:

- if character count exceeds `4000`, create a prominent warning

### 11.5 Root Analysis

Record:

- root name
- root namespace
- direct child element count
- whether direct children are all the same element type or mixed

### 11.6 Null and Empty Detection

Traverse every XML element and classify each occurrence as:

- has text content
- has child elements
- empty
- `xsi:nil="true"`

Rules:

- self-closing elements count as empty unless marked nil
- whitespace-only text should be treated as empty text
- `xsi:nil="true"` should count as NULL even if the element exists

For each null/empty occurrence, record:

- XPath
- parent element
- element name

### 11.7 Element Inventory

Collect:

- distinct element names
- count per element name
- maximum actual nesting depth
- observation list for all occurrences

### 11.8 Suggested Internal Helpers

- `readXmlBytes(Path)`
- `extractXmlDeclaration(byte[])`
- `detectEncoding(...)`
- `parseXmlDocument(...)`
- `traverseXmlElement(...)`
- `isWhitespaceOnly(String)`

## 12. Cross-Validator Detailed Instructions

This module compares analyzed XML against analyzed XSD.

### 12.1 Input

- `XsdAnalysisResult`
- `XmlAnalysisResult`

### 12.2 Output

- `ValidationResult`

### 12.3 Root Match Logic

Apply the exact business logic from the requirements:

1. If XML root equals XSD root:
   - report `PASS`
   - recommendation candidate is `is_top_level_element = 1`
2. Else if XML root equals XSD root's single repeating child:
   - report `PASS`
   - recommendation candidate is `is_top_level_element = 0`
3. Else:
   - report `FAIL`
   - include both names clearly

### 12.4 Required Field Null Check

For each XSD required element:

- find matching XML occurrences by path and/or element name
- determine whether any observed occurrence is null or empty
- if yes, add `CRITICAL`

Important design note:

Prefer path-aware matching over name-only matching when possible, because identical element names may appear in different branches.

### 12.5 Element Presence Check

Need three outcomes:

- XML element not in XSD -> `WARNING`
- required XSD element absent in XML -> `ERROR`
- optional XSD element absent in XML -> `INFO`

Implementation note:

For v1, compare using normalized element paths where practical.

### 12.6 Data Type Validation

Validate leaf text content against common XSD types:

- `xs:integer`
- `xs:date`
- `xs:dateTime`
- `xs:decimal`
- `xs:boolean`

Rules:

- skip validation for empty or nil values and let null-handling logic own that decision
- report mismatches with XPath, actual value, expected type

Suggested validators:

- integer: `Integer.parseInt` or `Long.parseLong` depending on desired tolerance
- decimal: `BigDecimal`
- date: `LocalDate` with strict `yyyy-MM-dd`
- dateTime: parse ISO-8601 using Java time APIs
- boolean: allow `true`, `false`, `1`, `0`

### 12.7 Namespace Consistency

Compare:

- root namespace
- element namespaces when available

If mismatched:

- add `WARNING`

## 13. Parameter Recommender Detailed Instructions

### 13.1 Input

- `XsdAnalysisResult`
- `XmlAnalysisResult`
- `ValidationResult`
- XML file name and XSD file name

### 13.2 Output

- `ParameterRecommendations`

### 13.3 Recommendation Rules

#### A. `is_top_level_element`

Choose:

- `1` if XML root matches XSD root
- `0` if XML root matches XSD single repeating child
- `AMBIGUOUS` otherwise

Always include explanation text.

#### B. `max_size`

Compute:

- actual character count
- buffered recommendation using the required formula

Add note:

- if sample data may be smaller than production, recommend sizing for largest expected document

#### C. `replace_null_string`

If any null or empty XML elements exist:

- recommend a replacement value such as empty string
- include affected XPaths

If none:

- recommend `NULL`

#### D. `xml_header`

If:

- encoding is UTF-8 and declaration is standard -> recommend `NULL`
- encoding is non-UTF-8 -> recommend exact declaration string
- no declaration exists -> recommend explicit header

#### E. `enable_validation`

If all cross-validation checks pass cleanly:

- recommend `1`

If any warning or error exists:

- recommend `0` until issues are resolved

Note:

This rule follows the Word document as written, even though a future version might want finer control.

## 14. Report Writer Detailed Instructions

The text report is the primary output and should match the Word document's content structure closely.

### 14.1 Required Section Order

1. Report Header
2. `SECTION A: XSD ANALYSIS`
3. `SECTION B: XML ANALYSIS`
4. `SECTION C: CROSS-VALIDATION`
5. `SECTION D: BODS PARAMETER RECOMMENDATIONS`
6. `SECTION E: ISSUES SUMMARY`
7. `SECTION F: PRE-FLIGHT STATUS`

### 14.2 Header Content

Must include:

- tool name
- version
- timestamp
- XML file path
- XSD file path

### 14.3 Text Output Rules

- keep labels stable
- align values for readability where practical
- preserve severity labels exactly
- use uppercase section labels
- stdout by default

### 14.4 Minimum Content Per Section

#### SECTION A

Include at least:

- root element
- single child element
- total elements
- required elements
- optional elements
- max nesting depth

#### SECTION B

Include at least:

- root element
- file size
- character count
- encoding
- null elements found

If verbose mode:

- list more detailed null/empty paths
- include distinct element inventory

#### SECTION C

Include at least:

- root match result
- required field null status
- unknown element status
- data type validation status
- namespace status if relevant

#### SECTION D

Include:

- `nested_table_name`
- `schema_dtd_name`
- `is_top_level_element`
- `max_size`
- `xml_header`
- `replace_null_string`
- `enable_validation`

Add explanatory notes in parentheses where useful.

#### SECTION E

Include counts for:

- `CRITICAL`
- `ERROR`
- `WARNING`
- `INFO`

#### SECTION F

Include exactly one overall status:

- `PASS`
- `PASS WITH WARNINGS`
- `FAIL`

### 14.5 Overall Status Rules

Use this logic:

- if any `CRITICAL` or `ERROR` exists -> `FAIL`
- else if warnings exist -> `PASS WITH WARNINGS`
- else -> `PASS`

### 14.6 HTML Writer

Do not implement until text output is stable unless explicitly requested earlier.

When implemented:

- mirror the same content structure
- do not invent different semantics

## 15. Exit Code Rules

Apply exactly:

- `0` -> pass, no critical or error issues
- `1` -> one or more critical or error issues
- `2` -> warnings only, but only when `--fail-on-warning` is set
- `3` -> execution error such as bad arguments, file missing, parse failure

Detailed logic:

1. If the tool cannot execute normally, return `3`
2. Else if report contains `CRITICAL` or `ERROR`, return `1`
3. Else if only warnings exist and `--fail-on-warning` is true, return `2`
4. Else return `0`

## 16. Error Handling Rules

All user-visible failures must be friendly.

### 16.1 Required Cases

- XML file missing -> clear message, exit `3`
- XSD file missing -> clear message, exit `3`
- XML parse failure -> clear message, line number if available, exit `3`
- XSD parse/schema failure -> clear message, detail if available, exit `3`
- bad arguments -> usage + clear message, exit `3`

### 16.2 User Experience Rule

Never print raw Java stack traces to normal users.

If internal logging is later needed, keep it separate from user-facing output.

## 17. Testing Strategy

Write tests in the same phased order as implementation.

### 17.1 Unit Tests for CLI

Test:

- required args
- missing values
- invalid format
- help behavior
- `--fail-on-warning`

### 17.2 XsdAnalyzer Tests

Cover:

- root element and no child wrapper -> recommendation candidate `1`
- root wraps one unbounded child -> recommendation candidate `0`
- multiple children -> ambiguous
- required vs optional fields
- namespace capture

### 17.3 XmlAnalyzer Tests

Cover:

- UTF-8 header
- ISO-8859-1 header
- no declaration
- under 4000 chars
- over 4000 chars
- nil elements
- empty self-closing elements
- nested hierarchy depth

### 17.4 CrossValidator Tests

Cover:

- root match to XSD root
- root match to XSD child
- root mismatch
- required null -> critical
- optional null -> info
- unknown XML element -> warning
- required missing element -> error
- type mismatch cases
- namespace mismatch

### 17.5 ParameterRecommender Tests

Cover:

- each parameter recommendation rule independently
- ambiguous top-level element case
- xml header output behavior
- enable validation behavior

### 17.6 TextReportWriter Tests

Verify:

- section order
- key labels present
- issue counts correct
- final status correct

### 17.7 End-to-End Tests

Prepare at least:

- passing pair
- required-null failure pair
- root mismatch failure pair
- large XML pair triggering max-size warning

## 18. Sample Data Guidance

Create curated sample fixtures early enough to support testing.

### 18.1 sample-pass

Should demonstrate:

- valid root relationship
- valid types
- no required nulls
- no unknown elements
- no warnings if possible

### 18.2 sample-fail

Should demonstrate at least one:

- required null
- root mismatch
- unknown element
- type mismatch

Consider having more than one fail sample even if the deliverables only name one folder.

## 19. Recommended Build Sequence in Practice

Follow these concrete steps when coding starts.

### Phase 1. Bootstrap

1. Create Maven project
2. Add Java 11 configuration
3. Add main class
4. Add basic README placeholder
5. Add package structure

Definition of done:

- project builds
- `java -jar` path is clear

### Phase 2. Shared Models

1. Implement `Severity`
2. Implement `Issue`
3. Implement result model classes
4. Implement constants

Definition of done:

- model classes compile
- no analyzer logic yet

### Phase 3. CLI

1. Implement `CommandLineOptions`
2. Implement `CommandLineParser`
3. Add help text
4. Add input validation

Definition of done:

- valid arguments parse cleanly
- invalid arguments return execution error behavior

### Phase 4. XSD Analyzer

1. Parse schema
2. identify root
3. traverse element definitions
4. compute required/optional stats
5. compute hierarchy depth
6. add warnings for weak typing

Definition of done:

- unit tests for core XSD scenarios pass

### Phase 5. XML Analyzer

1. read bytes
2. detect declaration and encoding
3. parse XML DOM
4. traverse all elements
5. classify null/empty
6. compute counts and depth
7. compute max-size warning input

Definition of done:

- unit tests for encoding/null/size cases pass

### Phase 6. Cross-Validation

1. compare roots
2. compare presence
3. check required-null conditions
4. run type checks
5. compare namespaces

Definition of done:

- unit tests for mismatch scenarios pass

### Phase 7. Parameter Recommendations

1. implement top-level recommendation logic
2. compute max-size recommendation
3. implement null replacement recommendation
4. implement xml header recommendation
5. implement enable-validation recommendation

Definition of done:

- all parameter tests pass

### Phase 8. Text Report Writer

1. render exact section order
2. render summaries
3. render issue counts
4. render overall status

Definition of done:

- report structure matches spec content model

### Phase 9. Orchestrator and Exit Codes

1. wire all modules together
2. merge issues into report
3. implement exit code rules
4. write stdout/file output paths

Definition of done:

- end-to-end run works on sample data

### Phase 10. HTML Output

1. implement only after text output is stable
2. reuse same report object
3. keep semantics identical to text writer

Definition of done:

- HTML report contains the same logical sections and values

## 20. Ambiguities to Resolve Before or During Coding

These are the main requirement areas where engineering judgment is needed.

### 20.1 Matching XML Elements to XSD Elements

Recommendation:

- use path-aware matching as primary
- fall back to name-based matching only when path matching is not possible
- if fallback was required, emit a warning in verbose mode

### 20.2 Multiple Top-Level Elements in XSD

Recommendation:

- if the schema exposes more than one plausible root, mark recommendation as ambiguous
- do not guess

### 20.3 Complex XSD Features

Recommendation:

- support common `sequence`-based schemas first
- emit warnings when encountering unsupported constructs

### 20.4 Exact XML Declaration Preservation

Recommendation:

- preserve the declaration string from the raw file when present
- do not reconstruct it from parsed DOM if exact text is required

### 20.5 Meaning of "Enable Validation"

The Word document says:

- recommend `1` only if all cross-validation checks pass
- recommend `0` if any warning or error exists

Recommendation:

- implement exactly that rule in v1

## 21. What Not to Do

Avoid these mistakes during implementation.

- do not mix report formatting logic into analyzer classes
- do not hardcode magic numbers outside constants
- do not expose Java exception internals to users
- do not assume all XML schemas are namespace-free
- do not rely only on element local names when path context is available
- do not make HTML the primary deliverable before text output is correct
- do not add external dependencies unless a real blocker appears

## 22. Acceptance Checklist

Before calling implementation complete, confirm:

- CLI accepts all required and optional arguments
- missing files return exit code `3`
- invalid XML returns exit code `3`
- invalid XSD returns exit code `3`
- required-null XML fields become `CRITICAL`
- unknown XML elements become `WARNING`
- missing required XSD elements become `ERROR`
- `is_top_level_element` recommendation follows the root logic
- `max_size` recommendation uses the exact formula
- `xml_header` recommendation follows encoding/declaration rules
- `replace_null_string` recommendation follows null detection results
- `enable_validation` recommendation follows validation results
- report sections appear in the required order
- final status is correct
- exit code is correct
- text output is readable and stable
- unit tests exist for analyzers
- at least one pass and one fail sample are included

## 23. Final Recommendation Before Coding

When coding begins, start with:

1. Maven skeleton
2. shared models
3. CLI parsing
4. text report scaffolding

Then build analyzers behind that structure.

This minimizes rework and gives every module a stable contract from the start.

## 24. Expanded Test Plan

This section turns testing into a first-class implementation requirement.

The goal is not only to prove the code works, but to prove:

- the rules from the Word document were implemented correctly
- the output structure remains stable over time
- regressions are caught early
- the command-line behavior is trustworthy

## 25. Test Philosophy

Use three layers of tests:

### 25.1 Rule-Level Unit Tests

These test focused logic with minimal setup.

Examples:

- severity ordering
- max-size formula
- boolean parsing rules
- date/dateTime validation
- exit code decision logic

These should be fast and numerous.

### 25.2 Module Tests

These test one class at a time using small fixture XML/XSD strings or files.

Examples:

- `XsdAnalyzer` given a small schema
- `XmlAnalyzer` given a small XML document
- `CrossValidator` given synthetic analysis results or paired fixtures

These should validate each module's contract in isolation.

### 25.3 End-to-End Tests

These run the application flow the way a real user would use it.

Examples:

- CLI invocation with `--xml` and `--xsd`
- report generation to stdout
- report generation to a file
- correct exit codes

These prove the entire tool works together.

## 26. Testing Principles

Follow these rules during implementation:

### 26.1 Test Structured Results First

Do not rely only on string comparisons for analyzers.

Prefer assertions like:

- root element name
- counts
- issue severity
- issue paths
- recommendation values

This makes tests stable and easier to diagnose.

### 26.2 Use Golden Output Tests Sparingly but Intentionally

The text report format matters, so keep a small number of golden-output tests for:

- section order
- headings
- labels
- overall report structure

Do not make every report test a full exact-output comparison, or the tests will become brittle.

### 26.3 Keep Fixtures Small and Purposeful

Every fixture should prove one main point.

Avoid giant XML/XSD samples for unit tests.

Use larger samples only for:

- performance checks
- end-to-end realism
- sample deliverables

### 26.4 Name Tests by Behavior

Prefer names like:

- `shouldRecommendTopLevelElementZeroWhenXmlRootMatchesSingleUnboundedChild`
- `shouldFlagRequiredFieldNullAsCritical`

Avoid vague names like:

- `testCase1`

## 27. Recommended Test Dependencies

If using Maven, recommended test stack:

- JUnit 5
- no additional assertion library required unless desired

Keep dependencies minimal unless a strong need appears.

Suggested categories:

- unit tests under `src/test/java`
- test resources under `src/test/resources`

## 28. Recommended Test Resource Layout

```text
src/
  test/
    java/
      com/example/bods/preflight/
        cli/
          CommandLineParserTest.java
          ExitCodePolicyTest.java
        analyzer/
          XsdAnalyzerTest.java
          XmlAnalyzerTest.java
          CrossValidatorTest.java
          ParameterRecommenderTest.java
        report/
          TextReportWriterTest.java
        integration/
          EndToEndTest.java
    resources/
      fixtures/
        xsd/
          root-direct.xsd
          root-wrapper-unbounded-child.xsd
          root-ambiguous.xsd
          required-optional-fields.xsd
          typed-fields.xsd
          namespace-schema.xsd
        xml/
          xml-root-matches-root.xml
          xml-root-matches-child.xml
          xml-required-null.xml
          xml-optional-empty.xml
          xml-unknown-element.xml
          xml-missing-required.xml
          xml-type-mismatch.xml
          xml-utf8.xml
          xml-iso-8859-1.xml
          xml-no-declaration.xml
          xml-over-4000.xml
          xml-namespace-mismatch.xml
        pairs/
          pass/
            sample.xml
            sample.xsd
          fail-required-null/
            sample.xml
            sample.xsd
          fail-root-mismatch/
            sample.xml
            sample.xsd
          warn-large-xml/
            sample.xml
            sample.xsd
      expected/
        reports/
          pass-report.txt
          fail-report.txt
```

## 29. Detailed Tests by Component

## 30. CLI and Entry-Point Tests

Create `CommandLineParserTest` and `ExitCodePolicyTest`.

### 30.1 CLI Parsing Cases

Test:

- valid required arguments parse successfully
- valid optional arguments parse successfully
- missing `--xml` fails
- missing `--xsd` fails
- missing value after `--output` fails
- invalid `--format` fails
- unknown argument fails
- `--help` sets help mode and avoids normal execution path
- explicit `--encoding UTF-8` is accepted
- invalid charset name is rejected

Assertions:

- parsed field values
- exception type or validation result
- friendly error message content if applicable

### 30.2 Exit Code Policy Cases

Test:

- no critical, no error, no warnings -> `0`
- warnings only with `failOnWarning=false` -> `0`
- warnings only with `failOnWarning=true` -> `2`
- any error -> `1`
- any critical -> `1`
- execution error path -> `3`

Implementation suggestion:

Put exit-code decision logic in a small dedicated helper so it can be unit tested independently.

## 31. Constants and Utility Tests

Create lightweight tests for utility logic.

### 31.1 Formula Tests

Test the exact formula:

`ceil(character_count * 1.2 / 1000) * 1000`

Cases:

- `0` characters -> expected lower bound behavior
- `1` character
- `3398` characters -> `5000`
- `4000` characters -> `5000`
- `4001` characters -> `5000`
- `9801` characters -> `12000`

Decide and document lower bound behavior clearly.

Recommendation:

- if the formula returns `0`, allow `0` internally unless the Word doc or future business rule says otherwise
- if a minimum nonzero output is preferred, define it explicitly in constants and test it

### 31.2 Type Validator Utility Tests

If type validation is extracted into helpers, test:

- integer valid/invalid
- decimal valid/invalid
- date valid/invalid
- dateTime valid/invalid
- boolean valid/invalid

## 32. XsdAnalyzer Test Matrix

Create focused schema fixtures and assert structured results.

### 32.1 Root Identification Tests

#### Case: Direct Root

Fixture:

- root element with no repeating wrapper child

Expect:

- root element captured correctly
- `rootHasSingleUnboundedChild = false`
- recommendation candidate `1` or direct-root-compatible result

#### Case: Wrapper Root with Single Unbounded Child

Fixture:

- root contains one child with `maxOccurs="unbounded"`

Expect:

- root element captured
- child element captured
- recommendation candidate `0`

#### Case: Ambiguous Root Structure

Fixture:

- root contains multiple meaningful children

Expect:

- ambiguous recommendation candidate
- warning issue if that is the chosen reporting style

### 32.2 Required and Optional Field Tests

Fixture:

- one schema with:
  - `minOccurs="1" nillable="false"`
  - `minOccurs="0"`
  - `nillable="true"`

Expect:

- required count correct
- optional count correct
- required flags correct per element

### 32.3 Data Type Extraction Tests

Fixture:

- schema with `xs:string`, `xs:integer`, `xs:date`, `xs:dateTime`, `xs:decimal`, `xs:boolean`

Expect:

- type names extracted correctly

### 32.4 Hierarchy and Depth Tests

Fixture:

- nested complex types with depth at least 4

Expect:

- correct max depth
- correct parent-child mapping

### 32.5 Warning Tests

Fixture:

- element with `xs:anyType`
- element without explicit type information where applicable

Expect:

- warning issues created

### 32.6 Namespace Tests

Fixture:

- schema with `targetNamespace` and prefixes

Expect:

- namespace declarations recorded
- root namespace captured

## 33. XmlAnalyzer Test Matrix

### 33.1 File-Level Tests

#### Case: UTF-8 with Declaration

Fixture:

- standard UTF-8 XML declaration

Expect:

- encoding `UTF-8`
- exact declaration string preserved

#### Case: ISO-8859-1 with Declaration

Fixture:

- non-UTF-8 declaration

Expect:

- encoding captured correctly
- declaration preserved exactly

#### Case: No Declaration

Fixture:

- XML with no declaration line

Expect:

- declaration is absent or null
- tool handles it gracefully

### 33.2 Size and Character Count Tests

Fixture:

- XML under 4000 chars
- XML over 4000 chars

Expect:

- correct character count
- correct size warning behavior

### 33.3 Root Structure Tests

Fixture:

- root with one repeated child type
- root with mixed child types

Expect:

- direct child count correct
- single-repeating-child flag correct

### 33.4 Null and Empty Detection Tests

Fixtures:

- self-closing element
- element with whitespace only
- element with `xsi:nil="true"`
- element with child nodes only
- element with non-empty text

Expect:

- each classification correct
- null and empty counts correct
- XPath recorded correctly

### 33.5 Element Inventory Tests

Fixture:

- repeating and nested elements

Expect:

- distinct element counts correct
- nesting depth correct
- observation list populated

### 33.6 Namespace Tests

Fixture:

- namespaced XML document

Expect:

- root namespace captured
- element namespace data preserved when available

## 34. CrossValidator Test Matrix

This is one of the highest-risk modules and deserves the strongest test coverage.

### 34.1 Root Matching Tests

#### Case: XML Root Matches XSD Root

Expect:

- root match pass
- child match false
- explanation indicates `is_top_level_element = 1`

#### Case: XML Root Matches XSD Root Child

Expect:

- root match pass through child logic
- explanation indicates `is_top_level_element = 0`

#### Case: XML Root Matches Neither

Expect:

- failure issue present
- both names clearly shown

### 34.2 Required Field Null Tests

Fixtures:

- required field empty
- required field `xsi:nil="true"`

Expect:

- `CRITICAL` issues created
- affected paths listed

### 34.3 Optional Null Tests

Fixture:

- optional field empty

Expect:

- `INFO` issue, not `CRITICAL`

### 34.4 Presence Comparison Tests

Fixtures:

- XML contains extra element not in XSD
- XML omits required XSD element
- XML omits optional XSD element

Expect:

- extra element -> `WARNING`
- missing required -> `ERROR`
- missing optional -> `INFO`

### 34.5 Data Type Validation Tests

Fixtures:

- integer field with text
- decimal field with invalid text
- date field in wrong format
- dateTime field not ISO-8601
- boolean field not one of accepted values

Expect:

- mismatch issue per invalid field
- issue contains XPath, expected type, actual value

### 34.6 Namespace Consistency Tests

Fixtures:

- matching namespace pair
- mismatching namespace pair

Expect:

- mismatch reported as warning

### 34.7 Path-Aware Matching Tests

Fixture:

- same element name appears in two different branches, only one branch is invalid

Expect:

- validator reports the correct branch
- name collisions do not produce false positives

This test is especially important.

## 35. ParameterRecommender Test Matrix

### 35.1 `is_top_level_element`

Test:

- root match -> `1`
- child match -> `0`
- ambiguous -> `AMBIGUOUS`

Assert:

- value
- explanation string

### 35.2 `max_size`

Test:

- correct actual character count included
- correct buffered value included
- explanation includes sample-size note where applicable

### 35.3 `replace_null_string`

Test:

- no nulls -> `NULL`
- nulls present -> replacement recommendation returned
- affected paths included

### 35.4 `xml_header`

Test:

- standard UTF-8 declaration -> `NULL`
- non-UTF-8 -> exact header string
- no declaration -> explicit-header recommendation

### 35.5 `enable_validation`

Test:

- no warnings/errors -> `1`
- warnings present -> `0`
- errors present -> `0`

## 36. Report Writer Test Matrix

Because content structure matters to you, report tests should be explicit.

### 36.1 Structural Tests

Verify generated text contains sections in this order:

1. header
2. section A
3. section B
4. section C
5. section D
6. section E
7. section F

Assertion method:

- compare index positions of headings in the final string

### 36.2 Label Tests

Verify required labels exist exactly:

- `SECTION A: XSD ANALYSIS`
- `SECTION B: XML ANALYSIS`
- `SECTION C: CROSS-VALIDATION`
- `SECTION D: BODS PARAMETER RECOMMENDATIONS`
- `SECTION E: ISSUES SUMMARY`
- `SECTION F: PRE-FLIGHT STATUS`

### 36.3 Content Inclusion Tests

Verify report includes:

- root element
- required/optional counts
- character count
- parameter recommendation lines
- severity totals
- overall status

### 36.4 Golden Report Tests

Use at least two exact or near-exact golden comparisons:

- pass case report
- fail case report

Recommendation:

- normalize timestamp before comparison
- use deterministic fixture paths if possible

This keeps comparisons stable.

## 37. End-to-End Integration Tests

These should simulate real usage as closely as practical.

### 37.1 Pass Case

Input:

- known-good XML/XSD pair

Expect:

- report status `PASS`
- exit code `0`

### 37.2 Fail Case: Required Null

Input:

- required field empty or nil

Expect:

- `CRITICAL` issue present
- overall `FAIL`
- exit code `1`

### 37.3 Fail Case: Root Mismatch

Input:

- XML root not matching XSD root or child

Expect:

- cross-validation fail
- exit code `1`

### 37.4 Warning Case with `--fail-on-warning`

Input:

- warning-only scenario

Expect:

- exit code `2` when flag present
- exit code `0` when flag absent

### 37.5 Execution Error Case

Input:

- missing file or invalid XML/XSD

Expect:

- exit code `3`
- friendly error message

### 37.6 Output File Case

Input:

- `--output <path>`

Expect:

- output file created
- contents match expected report structure

## 38. Suggested Fixture Inventory

Build fixtures intentionally. Each one should answer one question.

### 38.1 Core XSD Fixtures

- `root-direct.xsd`
- `root-wrapper-unbounded-child.xsd`
- `root-ambiguous.xsd`
- `required-optional-fields.xsd`
- `typed-fields.xsd`
- `namespace-schema.xsd`
- `anytype-warning.xsd`
- `deep-nesting.xsd`

### 38.2 Core XML Fixtures

- `xml-root-matches-root.xml`
- `xml-root-matches-child.xml`
- `xml-required-empty.xml`
- `xml-required-nil.xml`
- `xml-optional-empty.xml`
- `xml-unknown-element.xml`
- `xml-missing-required.xml`
- `xml-type-mismatch.xml`
- `xml-utf8.xml`
- `xml-iso-8859-1.xml`
- `xml-no-declaration.xml`
- `xml-over-4000.xml`
- `xml-namespace-match.xml`
- `xml-namespace-mismatch.xml`
- `xml-duplicate-element-name-different-path.xml`

### 38.3 Paired Integration Fixtures

- pass pair
- required-null fail pair
- root-mismatch fail pair
- warning-only pair
- large-xml warning pair

## 39. Performance and Robustness Tests

These do not need to be first, but should be considered before calling the project done.

### 39.1 Large XML Smoke Test

Generate or include an XML approaching the target scale.

Goal:

- ensure no catastrophic slowdown
- ensure memory remains practical

This does not need to be a strict benchmark at first.

### 39.2 Malformed Input Tests

Test:

- malformed XML
- malformed XSD
- empty file
- unreadable file if practical

Expect:

- execution error path
- user-friendly message

## 40. Test Implementation Order

When coding begins, build tests in this order:

1. constants and formula tests
2. CLI tests
3. XSD analyzer tests
4. XML analyzer tests
5. cross-validator tests
6. parameter recommender tests
7. report writer tests
8. end-to-end tests

Reason:

- this mirrors implementation order
- failures are easier to isolate

## 41. Test Completion Gates

Do not advance phases casually. Use these gates.

### Gate 1. Before Cross-Validation

Must pass:

- CLI tests
- XSD analyzer core tests
- XML analyzer core tests

### Gate 2. Before Report Writer Finalization

Must pass:

- cross-validator tests
- parameter recommender tests

### Gate 3. Before Declaring v1 Complete

Must pass:

- report structure tests
- golden output tests
- end-to-end exit code tests

## 42. Minimum Test Coverage Expectations

No exact coverage percentage is required, but these areas must be covered explicitly:

- root matching logic
- required vs optional determination
- null and empty classification
- type validation
- parameter recommendation logic
- report section order
- exit codes

If code coverage tooling is added later, use it as a signal, not as the primary quality measure.

## 43. Suggested Future Test Enhancements

Not required for first delivery, but worth considering:

- snapshot tests for HTML output
- property-based tests for numeric and date validators
- mutation testing for validation logic
- compatibility tests using real-world customer XML/XSD pairs

## 44. Final Testing Recommendation

When implementation starts, write tests alongside each phase instead of saving them for the end.

Best practice for this project:

1. write fixture
2. write failing test
3. implement logic
4. make test pass
5. move to next rule

That approach fits this tool well because the requirements are rule-heavy and output-sensitive.
