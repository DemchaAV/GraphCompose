package com.demcha.examples;

public final class GenerateAllExamples {

    private GenerateAllExamples() {
    }

    public static void main(String[] args) throws Exception {
        System.out.println("Generated: " + CvFileExample.generate());
        System.out.println("Generated: " + CoverLetterFileExample.generate());
        System.out.println("Generated: " + InvoiceFileExample.generate());
        System.out.println("Generated: " + ProposalFileExample.generate());
        System.out.println("Generated: " + WeeklyScheduleFileExample.generate());
    }
}
