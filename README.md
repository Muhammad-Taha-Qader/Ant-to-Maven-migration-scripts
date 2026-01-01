# Ant to Maven Migration – Dependency Automation Tools

This repository contains **small, focused Java utilities** designed to simplify one of the **most time-consuming parts of Ant → Maven migration**:

> 🔹 **Converting a legacy `lib/` folder full of JARs into proper Maven dependencies**

Instead of manually inspecting each JAR, searching online, and copying dependency snippets, these tools automate **classification and dependency generation**, reducing days of manual work to minutes.

---

## 🎯 Migration Problem This Solves

In legacy Ant-based projects, dependencies are typically managed as:

```
/lib
 ├── httpclient-4.5.14.jar
 ├── jackson-core-2.18.1.jar
 ├── i2c-custom-service.jar
 ├── jaxp-api.jar
 └── ...
```

During Maven migration, you must:

* Identify **3rd-party vs internal JARs**
* Identify dublicate jars with differnt versions - help make clean pom
* Convert versioned JARs into proper `<dependency>` entries
* Decide how to handle non-versioned / internal JARs (e.g., Nexus, replacements, upgrades)
* Analyzes transitive dependencies safely

This repository automates **exactly that phase**.

---

## 🧩 Tools Overview

### 1️⃣ `JarVersionSplitter.java`

**Purpose:**
Splits a raw list of JAR file names into:

* JARs **with versions** (usually 3rd-party)
* JARs **without versions** (usually internal / legacy)

#### Input

A text file containing JAR names (one per line):

```
all-jars.txt
```

Example:

```
httpclient-4.5.14.jar
jackson-core-2.18.1.jar
i2c-random-card-common.jar
jaxp-api.jar
```

#### Output

* `jars-with-version.txt`
* `jars-without-version.txt`

#### Console Summary

```
===== JAR Split Summary =====
Total jar names read     : 186
Jars WITH version        : 143
Jars WITHOUT version     : 43
================================
```

#### How to Run

```bash
javac JarVersionSplitter.java
java JarVersionSplitter
```

---



### 2️⃣ `CheckJarDuplicates.java`

**Purpose:**
A very simple way to detect duplicate JARs (same library name, different versions) from a .txt file.

#### 🔍 What counts as “duplicate” here?

- Same artifact name
- Different versions
- Example:
```
jboss-logging-3.3.0.Final.jar
jboss-logging-3.5.3.Final.jar
```

#### Input

A text file containing JAR names (one per line):

```
jars.txt
```

#### Output
```
DUPLICATE FOUND: commons-math3
  commons-math3-3.2.jar
  commons-math3-3.6.1.jar
-------------------------
DUPLICATE FOUND: jboss-logging
  jboss-logging-3.3.0.Final.jar
  jboss-logging-3.3.0.Final.jar
  jboss-logging-3.5.3.Final.jar
-------------------------
DUPLICATE FOUND: guava
  guava-33.3.1-android.jar
  guava-33.3.1-jre.jar
-------------------------
```

#### How to Run

```bash
javac CheckJarDuplicates.java
java CheckJarDuplicates
```

---





### 3️⃣ `dependency-generator` (Maven project)

**Purpose:**
Automatically generates **ready-to-paste Maven `<dependency>` blocks** for all versioned JARs using **Maven Central Search API**.

This eliminates:

* Manual Google searches
* Visiting mvnrepository.com
* Copy-pasting dependency snippets

#### Input

```
jars-with-version.txt
```

Each line:

```
artifactId-version.jar
```

Example:

```
FastInfoset-1.2.16.jar
jackson-core-2.18.1.jar
```

---

## 🏗 Project Structure

```
dependency-generator/
 ├── pom.xml
 └── src/main/java/
     └── MavenDependencyGenerator.java
```

---

## ▶ How to Run Dependency Generator

### Step 1: Compile

```bash
mvn compile
```

### Step 2: Execute

```bash
mvn exec:java
```

(Uses `exec-maven-plugin` configured in `pom.xml`)

---

## 📤 Output Files

### 1️ `generated-dependencies.xml`

Contains **ready-to-use Maven dependencies**:

```xml
<!-- FastInfoset-1.2.16.jar -->
<dependency>
    <groupId>com.sun.xml.fastinfoset</groupId>
    <artifactId>FastInfoset</artifactId>
    <version>1.2.16</version>
</dependency>
```

You can directly paste this into:

```xml
<dependencies>
    ...
</dependencies>
```

---

### 2️ `unresolved-jars.txt`

Contains JARs that:

* Could not be resolved via Maven Central
* Are likely internal, shaded, renamed, or legacy

These are intentionally left for **manual handling**, Nexus uploads, or upgrades.

---

## 📊 Console Summary Example

```
===== Maven Dependency Generation Summary =====
Total jars processed : 143
Resolved successfully: 131
Failed / unresolved  : 12
Output file          : generated-dependencies.xml
Unresolved jars file : unresolved-jars.txt
================================================
```

---

### 4️⃣ `MavenTreeAnalyzer.java`

**Purpose:**
Analyzes the output of `mvn dependency:tree` to identify **safe transitive dependencies** and highlight **only those transitive JARs that are NOT already present as direct (main) dependencies**.

This step is typically used **after initial dependency generation**, once a working `pom.xml` exists.

It helps:

* Avoid redundant dependency declarations
* Detect hidden transitive-only libraries
* Prevent accidental version / artifact duplication
* Make dependency cleanup decisions confidently

In simple terms, you can exclude all transitive dependencies except those flagged by this utility. This utility only lists the jars that are not conflicting, and therefore should not be excluded blindly.

#### Input

A Maven dependency tree output file:

```
mvn dependency:tree > tree.txt
```

#### Analysis Strategy

The analyzer produces **two reports**:

**Report 1 – Strict comparison**

* Conflict check uses **groupId + artifactId**
* Versions are ignored intentionally

**Report 2 – Relaxed comparison**

* Conflict check uses **artifactId only**
* groupId and version are ignored
* Useful for detecting relocated or repackaged libraries (e.g. `jsch`)

#### Key Assumptions

* Tree is generated using default Maven formatting
* Lines starting with `+-` are main dependencies
* Lines starting with `|  \\` or `|  +-` are transitive dependencies

#### Output Example

```
REPORT 1: Missing transitive deps (groupId + artifactId comparison)
org.apache.ant:ant-jsch
  -> com.jcraft:jsch

REPORT 2: Missing transitive deps (artifactId-only comparison)
(No entry shown if artifact already exists as a main dependency)
```













---
#### 🔍 Example: How Transitive Dependencies Are Highlighted

Consider the following dependency tree snippet:

```text
[INFO] +- org.apache.ant:ant-jsch:jar:1.10.15:compile
[INFO] |  \- com.jcraft:jsch:jar:0.1.55:compile
[INFO] +- org.lucee:jsch:jar:0.1.55:compile
````

Here:

* `org.apache.ant:ant-jsch` brings `com.jcraft:jsch` as a **transitive dependency**
* The same JAR (`jsch`) already exists as a **direct dependency**, but under a **different groupId**



##### Report 1 – Strict (groupId + artifactId)

**Comparison rule:**
`groupId + artifactId` (version ignored)

**Result:**

```text
org.apache.ant:ant-jsch
  -> com.jcraft:jsch
```

This appears because:

* `com.jcraft:jsch` ≠ `org.lucee:jsch`
* GroupId difference is treated as a conflict

This report is useful for:

* Precise dependency hygiene
* Detecting relocated or forked artifacts



##### Report 2 – Relaxed (artifactId only)

**Comparison rule:**
`artifactId` only (groupId and version ignored)

**Result:**

```text
(no entry reported)
```

This is because:

* `jsch` already exists as a direct dependency
* The transitive dependency does not introduce a *new* JAR by name

This report is useful for:

* Avoiding false positives
* Handling repackaged / shaded / relocated libraries
* High-level cleanup decisions
---













#### How to Run

```bash
javac MavenTreeAnalyzer.java
java MavenTreeAnalyzer tree.txt
```

---

## ✅ What This Repo Covers in Ant → Maven Migration

✔ Classifies legacy JARs
✔ Converts versioned JARs into Maven dependencies
✔ Analyzes transitive dependencies safely
✔ Prevents duplicate or conflicting JAR inclusion
✔ Reduces manual dependency research by ~90%
✔ Produces audit-friendly output

---

## ❌ What This Repo Does NOT Do (By Design)

* Upload internal JARs to Nexus
* Guess dependency scopes (`test`, `provided`, etc.)
* Resolve shaded / renamed vendor JARs automatically
* Replace obsolete libraries

These steps require **architectural decisions** and are intentionally kept manual.

---

## 🧠 Intended Usage

This tooling is meant to be used:

* Early-to-mid Ant → Maven migration
* As a **one-time conversion & validation helper**
* By developers modernizing large legacy Java systems

---

## 🚀 Future Enhancements (Optional)

* Automatic `<exclusion>` generation
* Dependency version conflict detection
* CSV / Excel export for review
* DependencyManagement block generation

---

## 📌 Summary

This repository automates the **most repetitive, error-prone, and review-heavy parts** of Ant → Maven migration:

> Turning a legacy `lib/` folder and dependency tree into a clean, maintainable, and conflict-free Maven dependency setup.
