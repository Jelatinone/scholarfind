# Scholarfind\*

_"Scholarfind is a resource for connecting student profiles with scholarships, all at no cost." - Me, right now_ :)

> [!IMPORTANT]
> 'Scholarfind' is a placeholder name for this project. If you have a name suggestion, please open an issue!

## I. A long time ago in a galaxy far, far away...

Like many others, during my senior year of high school affording college became a very real concern I hadn't really thought about. Like many others, I turned to scholarships. Like many others, I found the process fragmented, repetitive, and inefficient.

_Unlike many others, I set about trying to change that._

## II. The Intolerable Acts

**Discoverable**

You can't apply for what you never found. The most prevalent problem, for any student is simply discovering the scholarships. While many hubs for scholarships exist, scholarship data is fragmented across thousands of unstructured sources. There is no centralized system. Opportunities are often time-sensitive but aren't surfaced in real-time.

**Structured**

Structure is everything when applying to hundreds of sources, want to spend hours building your own spreadsheet tracking a hundred applications? No, you don't (I hope). Critical information is often buried or scattered, inconsistent and non-standardized, duplicated, outdated, invalid. Content can exist in multiple formats: `.html`, `.pdf`, `.docx`, et al. Many eligibility rules and requirements are implied in descriptions or summaries rather than made explicitly structural; our interpretations of natural language introduce ambiguity and inconsistency.

**Searchable**

Existing platforms often rely on rigid, rule-based filters that fail under nuanced eligibility requirements: there lay a disconnect between the data and the user. Want to find scholarships applicable to students who love robotics? Great, here's fifty results for students who love robotics--who live in Narnia and attend Hogwarts. Users don't want to sift through large volumes of entirely irrelevant results.

**Frictionless**

The most annoying? The need for an account, EVERYWHERE. Want to sign-up with our service? Just make another account among the hundreds you already have. Just spend another thirty minutes to input the same information across another platform. Results are not personalized without significant user effort. Not to mention a new UI to learn, poorly optimized interfaces, or poor design choices.

### III. The Heist

So, on a cold and stormy night, I set about solving these issues:

_What if I built a unified scholarship engine which scraped publicly available content to generate high-fidelity structured data?_

Instead of needing to split across several different hubs for scholarships, build a single unified hub that derived from the hundreds of sources out there? Leveraging the abundance of duplicates to create a highly-accurate model in real-time with frequent, dynamic re-discovery from sources.

Normalize the structure with a single model, a standardized scholarship that is a wide as the ocean, but strict enough that we can display normalized content without issues.

Allow users to create a short profile to query with, instead of querying by a single factor (location, amount, name), query by matching the aspects of a student profile. And instead of a binary matching algorithm take advantage of the bounds of the scholarship model. Want scholarships that apply to marine biologists in their masters but excluding results with a minimum GPA? _Perfect._

At no point during the process do we ever create an account, save user data, or sell it to third parties.

## IV. What Scholarfind\* Is

Scholarfind\* is a staged pipeline for discovering, normalizing, classifying, and eventually publishing scholarship data from public sources.

Today, this repository contains the core backend foundation:

| Module        | Purpose                                                                          |
| ------------- | -------------------------------------------------------------------------------- |
| `common`      | Shared models, pipeline framework, policy engine, and acquisition logic          |
| `infra`       | AWS-backed infrastructure implementations, queues, stores, and serializers       |
| `investigate` | Progressive source classification using signal extraction and staged acquisition |

Planned modules still exist in the project vision, and have some related implementation (see `infra`, `common.model`), even if they are not yet checked into this repository in full:

| Planned Stage | Purpose                                                              |
| ------------- | -------------------------------------------------------------------- |
| `annotate`    | Deep extraction and full scholarship classification                  |
| `publish`     | Merge, promote, and update normalized scholarship records            |
| `discovery`   | Find new sources, revisit stale records, and recover malformed flows |
| `frontend`    | User-facing search and scholarship exploration                       |

## V. System Overview

The Scholarfind\* architecture is built around stage-oriented services operating in an asynchronous queue-driven pipeline.

Each stage is responsible for three things:

1. Defining the infrastructure it depends on
2. Applying policies that govern business rules and transient behavior
3. Producing strongly typed stage documents and emissions for the next stage

Logically, the pipeline looks like this:

| Stage       | Purpose                                                                      |
| ----------- | ---------------------------------------------------------------------------- |
| Investigate | Lightweight source classification and elimination round                      |
| Annotation  | Full content extraction and complete scholarship interpretation              |
| Publish     | Record updates, merge logic, and normalized output publication               |
| Discovery   | Find new sources, stale records, and malformed records                       |

## VI. At A Glance

Current repository status:

| Module        | Status |
| ------------- | ------ |
| `common`      | 🛠️     |
| `infra`       | ✅     |
| `investigate` | ✅     |
| `annotate`    | 🛠️     |
| `publish`     | ❌     |
| `discovery`   | ❌     |
| `frontend`    | ❌     |

Notable existing capabilities by module:

### `common`

- Task engine
- Policy engine
- Acquisition engine
- Shared models
- Shared infrastructure contracts

### `infra`

- AWS infrastructure implementations
- Queue abstractions and concrete SQS adapters
- Dynamo and S3-backed store implementations
- Jackson serializers for queue and persistence payloads

### `investigate`

- Signal-driven classification planner
- Progressive acquisition-aware evidence gathering
- Classification reuse policies
- Investigate outcome routing
- Signal extractors, evidence rules, and score configuration

## VII. Development

### Requirements

- Java 21+

### Useful Commands

Run the full test suite:

```powershell
.\gradlew.bat test
```

Build everything:

```powershell
.\gradlew.bat build
```

The root build also applies Spotless formatting automatically as part of `build`.

## Appendix A: Why This Exists

This is less "just scrape some web pages" and more "build a durable pipeline that survives the real internet."

Because scholarship discovery should not be fragmented, opaque, and exhausting by default.

Because students should not need ten accounts and three spreadsheets to find financial aid.

Because there is a real opportunity to take messy public data and make it searchable, structured, and useful without making the user jump through hoops first.

And because, frankly, this sounded fun.

## Appendix B: Prior Development

Prior to complete development, I completed a lightweight local prototype implementation of Scholarfind\* [available on my profile](https://github.com/Jelatinone/Scholarfind-Prototype).
