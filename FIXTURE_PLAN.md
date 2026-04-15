# BODS XML Pre-Flight Checker Fixture Plan

This file defines the test fixtures to create before or during implementation.

Its purpose is to make the testing strategy concrete by specifying:

- which XML and XSD files should exist
- what each fixture is designed to prove
- which tests should use each fixture
- what outcomes are expected from each fixture

This file works together with:

- [IMPLEMENTATION_HELPER.md](C:/data/Github/xml/IMPLEMENTATION_HELPER.md)
- `C:\Users\achan\Downloads\BODS_XML_PreFlight_Requirements.docx`

If there is any conflict, the Word document remains the source of truth.

## 1. Fixture Design Goals

The fixtures should be:

- small
- readable
- targeted
- reusable
- deterministic

Each fixture should ideally prove one main rule. A few integration fixtures can prove multiple rules at once, but unit-level fixtures should stay narrow.

## 2. Directory Layout

Recommended test fixture layout:

```text
src/test/resources/fixtures/
  xsd/
  xml/
  pairs/
    pass/
    fail-required-null/
    fail-root-mismatch/
    warn-large-xml/
    warn-unknown-element/
    perf-large/
```

Recommended deliverable sample layout:

```text
sample-pass/
  sample.xml
  sample.xsd

sample-fail/
  sample.xml
  sample.xsd
```

Note:

- The `src/test/resources/fixtures` area is for automated tests.
- The `sample-pass` and `sample-fail` folders are end-user deliverables.

## 3. Fixture Naming Rules

Use descriptive names.

Good examples:

- `root-wrapper-unbounded-child.xsd`
- `xml-required-nil.xml`
- `xml-unknown-element.xml`

Avoid generic names:

- `test1.xml`
- `sample2.xsd`

## 4. Core XSD Fixtures

## 5. `root-direct.xsd`

Purpose:

- prove the schema root itself is the expected logical top-level element

Design:

- one top-level root element
- no wrapper pattern with single repeating child
- a few leaf children

Should include:

- one required string field
- one optional string field

Expected use:

- `XsdAnalyzerTest`
- root recommendation tests
- baseline integration tests

Expected outcomes:

- root element identified correctly
- `rootHasSingleUnboundedChild = false`
- recommendation candidate compatible with `is_top_level_element = 1`

## 6. `root-wrapper-unbounded-child.xsd`

Purpose:

- prove detection of wrapper root plus repeating child

Design:

- root element like `allBillingAddresses`
- one child like `billingAddress`
- child marked `maxOccurs="unbounded"`

Should include:

- required fields within child
- optional fields within child

Expected use:

- wrapper root tests
- cross-validation child-match tests
- recommender tests for `is_top_level_element = 0`

Expected outcomes:

- root identified
- single child identified
- child maxOccurs captured as unbounded
- recommendation candidate `0`

## 7. `root-ambiguous.xsd`

Purpose:

- prove ambiguous root detection

Design:

- root has multiple significant child elements
- no obvious single repeating child pattern

Expected use:

- ambiguity handling tests

Expected outcomes:

- no confident `0` or `1`
- ambiguous result or warning

## 8. `required-optional-fields.xsd`

Purpose:

- prove required-vs-optional classification

Design:

- one field with `minOccurs="1"` and `nillable="false"`
- one field with `minOccurs="0"`
- one field with `nillable="true"`

Expected use:

- `XsdAnalyzerTest`
- `CrossValidatorTest`

Expected outcomes:

- required count correct
- optional count correct
- required flag true only where intended

## 9. `typed-fields.xsd`

Purpose:

- prove data type extraction and type validation mapping

Design:

- leaf fields with types:
  - `xs:string`
  - `xs:integer`
  - `xs:decimal`
  - `xs:date`
  - `xs:dateTime`
  - `xs:boolean`

Expected use:

- `XsdAnalyzerTest`
- `CrossValidatorTest`
- `ParameterRecommenderTest`

Expected outcomes:

- types extracted exactly
- validator can pair XML values against these types

## 10. `namespace-schema.xsd`

Purpose:

- prove namespace capture and namespace comparison support

Design:

- schema with `targetNamespace`
- prefix declarations
- namespaced root and children

Expected use:

- namespace tests

Expected outcomes:

- namespace declarations collected
- root namespace captured

## 11. `anytype-warning.xsd`

Purpose:

- prove warnings for weak typing

Design:

- include `xs:anyType`
- optionally include a schema element that lacks useful typing info

Expected use:

- warning behavior tests

Expected outcomes:

- warning issue created for `xs:anyType`

## 12. `deep-nesting.xsd`

Purpose:

- prove hierarchy depth computation

Design:

- nested structure at least 4 levels deep

Expected use:

- depth tests

Expected outcomes:

- max nesting depth matches intended schema depth

## 13. Core XML Fixtures

## 14. `xml-root-matches-root.xml`

Purpose:

- prove XML root matching to XSD root

Design:

- XML root name exactly equals the direct schema root

Expected use:

- `CrossValidatorTest`
- recommender tests

Expected outcomes:

- root match pass
- recommendation path supports `is_top_level_element = 1`

## 15. `xml-root-matches-child.xml`

Purpose:

- prove XML root matching XSD root's repeating child

Design:

- XML document root equals the child element from `root-wrapper-unbounded-child.xsd`

Expected use:

- child-match validator tests

Expected outcomes:

- child match pass
- recommendation path supports `is_top_level_element = 0`

## 16. `xml-required-empty.xml`

Purpose:

- prove empty required element detection

Design:

- include a required element as self-closing or whitespace-only

Expected use:

- XML analyzer tests
- cross-validator critical issue tests

Expected outcomes:

- element classified as empty
- if paired to required XSD element, reported as `CRITICAL`

## 17. `xml-required-nil.xml`

Purpose:

- prove `xsi:nil="true"` handling

Design:

- namespaced `xsi:nil="true"` on a required element

Expected use:

- null classification tests
- required-null critical tests

Expected outcomes:

- element classified as null
- cross-validator marks as `CRITICAL` if required

## 18. `xml-optional-empty.xml`

Purpose:

- prove optional empty handling

Design:

- optional field present but empty

Expected use:

- optional null behavior tests

Expected outcomes:

- XML analyzer flags empty
- validator emits `INFO`, not `CRITICAL`

## 19. `xml-unknown-element.xml`

Purpose:

- prove detection of XML elements absent from schema

Design:

- include one extra leaf element not present in matching XSD

Expected use:

- presence mismatch tests

Expected outcomes:

- `WARNING` for unknown XML element

## 20. `xml-missing-required.xml`

Purpose:

- prove detection of required XSD elements absent from XML

Design:

- omit a required field entirely

Expected use:

- cross-validator presence tests

Expected outcomes:

- `ERROR` for missing required element

## 21. `xml-type-mismatch.xml`

Purpose:

- prove type validation issues

Design:

- invalid integer
- invalid decimal
- invalid date
- invalid dateTime
- invalid boolean

Expected use:

- data type validation tests

Expected outcomes:

- one mismatch issue per invalid field

## 22. `xml-utf8.xml`

Purpose:

- prove UTF-8 declaration handling

Design:

- standard XML declaration with `encoding="UTF-8"`

Expected use:

- XML analyzer encoding tests
- recommender tests

Expected outcomes:

- encoding captured as UTF-8
- `xml_header` recommendation can be `NULL`

## 23. `xml-iso-8859-1.xml`

Purpose:

- prove non-UTF-8 declaration handling

Design:

- XML declaration with `encoding="ISO-8859-1"`

Expected use:

- XML analyzer encoding tests
- recommender tests

Expected outcomes:

- encoding captured correctly
- exact declaration preserved
- `xml_header` recommendation uses explicit header

## 24. `xml-no-declaration.xml`

Purpose:

- prove no-declaration handling

Design:

- valid XML file without XML declaration

Expected use:

- XML analyzer tests
- recommender tests

Expected outcomes:

- no crash
- declaration recorded as absent
- recommendation suggests explicit header

## 25. `xml-over-4000.xml`

Purpose:

- prove large-document warning and max-size recommendation logic

Design:

- valid XML with total character count just over 4000

Expected use:

- XML analyzer tests
- recommender tests
- warning integration tests

Expected outcomes:

- character count > 4000
- warning present
- buffered `max_size` value correct

## 26. `xml-namespace-match.xml`

Purpose:

- prove namespace-consistent pairing

Design:

- XML using the same namespace as `namespace-schema.xsd`

Expected use:

- namespace consistency tests

Expected outcomes:

- no namespace warning

## 27. `xml-namespace-mismatch.xml`

Purpose:

- prove namespace inconsistency detection

Design:

- XML root namespace differs from schema target namespace

Expected use:

- namespace mismatch tests

Expected outcomes:

- namespace mismatch warning

## 28. `xml-duplicate-element-name-different-path.xml`

Purpose:

- prove path-aware matching rather than name-only matching

Design:

- same element name appears in two different branches
- only one branch violates type or null rules

Expected use:

- advanced cross-validator tests

Expected outcomes:

- validator reports the correct branch only

This fixture is important because it protects against false positives.

## 29. Integration Fixture Pairs

These should combine XSD and XML files into realistic scenarios.

## 30. `pairs/pass/`

Purpose:

- clean end-to-end passing scenario

Contents:

- `sample.xsd`
- `sample.xml`

Recommended structure:

- use a wrapper or direct root design that is easy to understand
- no nulls in required fields
- no extra elements
- valid types
- namespace choice can be simple unless namespace support is being tested here

Expected outcomes:

- no `CRITICAL`
- no `ERROR`
- ideally no `WARNING`
- overall status `PASS`
- exit code `0`

## 31. `pairs/fail-required-null/`

Purpose:

- end-to-end fail caused by required null or empty value

Contents:

- `sample.xsd`
- `sample.xml`

Expected outcomes:

- one or more `CRITICAL`
- overall status `FAIL`
- exit code `1`

## 32. `pairs/fail-root-mismatch/`

Purpose:

- end-to-end fail caused by root mismatch

Contents:

- `sample.xsd`
- `sample.xml`

Expected outcomes:

- root match failure clearly reported
- overall status `FAIL`
- exit code `1`

## 33. `pairs/warn-large-xml/`

Purpose:

- warning-only scenario driven by large XML size

Contents:

- `sample.xsd`
- `sample.xml`

Expected outcomes:

- no critical/error
- warning about default `4000`
- overall status `PASS WITH WARNINGS`
- exit code:
  - `0` without `--fail-on-warning`
  - `2` with `--fail-on-warning`

## 34. `pairs/warn-unknown-element/`

Purpose:

- warning-only scenario driven by XML containing extra schema-unknown element

Contents:

- `sample.xsd`
- `sample.xml`

Expected outcomes:

- warning for unknown element
- overall status `PASS WITH WARNINGS`

## 35. `pairs/perf-large/`

Purpose:

- larger-file smoke testing for parsing, traversal, and `max_size` recommendation behavior

Contents:

- `sample.xsd`
- `sample.xml`

Design:

- schema aligned with the same business structure as the other billing address fixtures
- XML with a very large `notes` element so the total file is materially larger than the warning-only threshold

Expected outcomes:

- valid XML/XSD pairing
- no root mismatch
- no required-field null failure
- no unknown-element warning unless intentionally introduced later
- large character count and large `max_size` recommendation

Use cases:

- parser stability testing
- basic performance smoke testing
- report readability with large size values
- future non-functional requirement checks

## 35. Deliverable Sample Fixtures

These are not only test fixtures. They are part of the user-facing deliverables.

## 36. `sample-pass/`

Should be human-readable and simple enough to demonstrate a successful run.

Recommended content:

- root-child pattern that makes the report easy to understand
- at least one required field
- at least one optional field
- valid types

Expected report characteristics:

- all major sections populated
- recommendations easy to inspect
- final status `PASS`

## 37. `sample-fail/`

Should demonstrate an obvious but realistic failure.

Recommended content:

- one required field empty or nil
- optionally one additional warning

Expected report characteristics:

- at least one `CRITICAL`
- final status `FAIL`

## 38. Golden Output Fixture Strategy

For report tests, define a small set of stable fixture/report pairs.

Recommended golden tests:

- pass pair -> `pass-report.txt`
- fail-required-null pair -> `fail-report.txt`

Golden report guidance:

- normalize timestamp before comparison
- use stable relative file paths in tests if possible
- avoid comparing machine-specific absolute paths unless intentionally normalized

## 39. Minimum Fixture Set for First Coding Pass

If we want to start lean, create these first:

1. `root-wrapper-unbounded-child.xsd`
2. `required-optional-fields.xsd`
3. `typed-fields.xsd`
4. `xml-root-matches-child.xml`
5. `xml-required-empty.xml`
6. `xml-type-mismatch.xml`
7. `xml-utf8.xml`
8. `xml-over-4000.xml`
9. `pairs/pass/`
10. `pairs/fail-required-null/`

This is enough to begin meaningful implementation and testing.

## 40. Preferred Fixture Creation Order

Create fixtures in this order:

1. XSD fixtures for root logic
2. XSD fixture for type coverage
3. XML fixtures for null and encoding
4. XML fixture for type mismatches
5. paired pass fixture
6. paired fail fixture
7. warning-only fixture pairs
8. performance-large fixture
9. namespace fixtures
10. path-collision fixture

## 41. Fixture Authoring Rules

Follow these conventions when writing the actual XML/XSD files:

- use consistent indentation
- prefer UTF-8 unless testing another encoding
- keep element names readable and business-like
- keep namespaces simple and explicit
- do not reuse one giant schema for every scenario
- document the purpose of each fixture in a short comment if appropriate

## 42. Traceability Table

Use this table to map fixtures to requirement areas.

| Requirement Area | Primary Fixtures |
| :---- | :---- |
| Root element detection | `root-direct.xsd`, `root-wrapper-unbounded-child.xsd`, `xml-root-matches-root.xml`, `xml-root-matches-child.xml` |
| Required vs optional fields | `required-optional-fields.xsd`, `xml-required-empty.xml`, `xml-optional-empty.xml` |
| Null detection | `xml-required-empty.xml`, `xml-required-nil.xml` |
| Element presence checks | `xml-unknown-element.xml`, `xml-missing-required.xml` |
| Data type validation | `typed-fields.xsd`, `xml-type-mismatch.xml` |
| Encoding and XML header | `xml-utf8.xml`, `xml-iso-8859-1.xml`, `xml-no-declaration.xml` |
| max_size logic | `xml-over-4000.xml` |
| Large-file smoke testing | `pairs/perf-large/` |
| Namespace consistency | `namespace-schema.xsd`, `xml-namespace-match.xml`, `xml-namespace-mismatch.xml` |
| Path-aware matching | `xml-duplicate-element-name-different-path.xml` |
| End-to-end PASS | `pairs/pass/` |
| End-to-end FAIL | `pairs/fail-required-null/`, `pairs/fail-root-mismatch/` |

## 43. Final Recommendation

Before coding begins, review this fixture plan and confirm:

- the initial fixture set is enough
- the business-like sample names make sense
- the pass/fail examples match the kinds of XML/XSD files you expect in the real world

Once approved, these fixtures can be created alongside the Java implementation and used to drive development safely.
