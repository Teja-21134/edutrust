package com.edutrust.config;

import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.stereotype.Component;

import java.util.ArrayList;
import java.util.List;

@Component
@ConfigurationProperties(prefix = "edutrust.institution")
public class InstitutionProperties {

    private String name;
    private List<Department> departments = new ArrayList<>();
    private String allDepartmentsLabel = "All";
    private List<DocumentType> documentTypes = new ArrayList<>();
    private int defaultAuthorityRank = 1;
    private List<String> latestWords = new ArrayList<>();
    private ParityTerms parityTerms = new ParityTerms();
    private List<String> overridePhrases = new ArrayList<>();
    private String academicYearPattern = "\\d{4}-\\d{2}";
    private Messages messages = new Messages();

    public String getName() { return name; }
    public void setName(String name) { this.name = name; }
    public List<Department> getDepartments() { return departments; }
    public void setDepartments(List<Department> departments) { this.departments = departments; }
    public String getAllDepartmentsLabel() { return allDepartmentsLabel; }
    public void setAllDepartmentsLabel(String allDepartmentsLabel) { this.allDepartmentsLabel = allDepartmentsLabel; }
    public List<DocumentType> getDocumentTypes() { return documentTypes; }
    public void setDocumentTypes(List<DocumentType> documentTypes) { this.documentTypes = documentTypes; }
    public int getDefaultAuthorityRank() { return defaultAuthorityRank; }
    public void setDefaultAuthorityRank(int defaultAuthorityRank) { this.defaultAuthorityRank = defaultAuthorityRank; }
    public List<String> getLatestWords() { return latestWords; }
    public void setLatestWords(List<String> latestWords) { this.latestWords = latestWords; }
    public ParityTerms getParityTerms() { return parityTerms; }
    public void setParityTerms(ParityTerms parityTerms) { this.parityTerms = parityTerms; }
    public List<String> getOverridePhrases() { return overridePhrases; }
    public void setOverridePhrases(List<String> overridePhrases) { this.overridePhrases = overridePhrases; }
    public String getAcademicYearPattern() { return academicYearPattern; }
    public void setAcademicYearPattern(String academicYearPattern) { this.academicYearPattern = academicYearPattern; }
    public Messages getMessages() { return messages; }
    public void setMessages(Messages messages) { this.messages = messages; }

    public static class Department {
        private String code;
        private List<String> aliases = new ArrayList<>();
        public String getCode() { return code; }
        public void setCode(String code) { this.code = code; }
        public List<String> getAliases() { return aliases; }
        public void setAliases(List<String> aliases) { this.aliases = aliases; }
    }

    public static class DocumentType {
        private String name;
        private int authorityRank;
        private List<String> keywords = new ArrayList<>();
        public String getName() { return name; }
        public void setName(String name) { this.name = name; }
        public int getAuthorityRank() { return authorityRank; }
        public void setAuthorityRank(int authorityRank) { this.authorityRank = authorityRank; }
        public List<String> getKeywords() { return keywords; }
        public void setKeywords(List<String> keywords) { this.keywords = keywords; }
    }

    public static class ParityTerms {
        private List<String> odd = new ArrayList<>();
        private List<String> even = new ArrayList<>();
        public List<String> getOdd() { return odd; }
        public void setOdd(List<String> odd) { this.odd = odd; }
        public List<String> getEven() { return even; }
        public void setEven(List<String> even) { this.even = even; }
    }

    public static class Messages {
        private String notFound;
        private String conflictUnresolved;
        private String unverified;
        public String getNotFound() { return notFound; }
        public void setNotFound(String notFound) { this.notFound = notFound; }
        public String getConflictUnresolved() { return conflictUnresolved; }
        public void setConflictUnresolved(String conflictUnresolved) { this.conflictUnresolved = conflictUnresolved; }
        public String getUnverified() { return unverified; }
        public void setUnverified(String unverified) { this.unverified = unverified; }
    }
}
