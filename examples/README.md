# GraphCompose Examples

Runnable file-render examples for the main document scenarios:

- CV
- cover letter
- invoice
- proposal

## Workflow

1. Install the root library artifact from the repository root:

```powershell
mvn -DskipTests install
```

2. Run all examples from this module:

```powershell
mvn -f examples/pom.xml exec:java -Dexec.mainClass=com.demcha.examples.GenerateAllExamples
```

3. Generated PDFs are written to:

```text
examples/target/generated-pdfs/
```

You can also run a single example by changing `exec.mainClass` to one of:

- `com.demcha.examples.CvFileExample`
- `com.demcha.examples.CoverLetterFileExample`
- `com.demcha.examples.InvoiceFileExample`
- `com.demcha.examples.ProposalFileExample`
