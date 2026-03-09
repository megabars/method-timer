package com.github.methodtimer.agent;

import org.junit.jupiter.api.Test;

import java.lang.reflect.Method;
import java.util.List;

import static org.junit.jupiter.api.Assertions.*;

class TimingResultWriterTest {

    // ── toJson ────────────────────────────────────────────────────────────────

    @Test
    void toJson_normalFqn() {
        String json = TimingResultWriter.toJson("com.example.Foo.bar(java.lang.String, int)", 12345L);
        assertEquals("{\"fqn\":\"com.example.Foo.bar(java.lang.String, int)\",\"timeNs\":12345}", json);
    }

    @Test
    void toJson_escapesDoubleQuote() {
        String json = TimingResultWriter.toJson("a\"b", 1L);
        assertEquals("{\"fqn\":\"a\\\"b\",\"timeNs\":1}", json);
    }

    @Test
    void toJson_escapesBackslash() {
        String json = TimingResultWriter.toJson("a\\b", 1L);
        assertEquals("{\"fqn\":\"a\\\\b\",\"timeNs\":1}", json);
    }

    @Test
    void toJson_escapesNewline() {
        String json = TimingResultWriter.toJson("a\nb", 1L);
        assertEquals("{\"fqn\":\"a\\nb\",\"timeNs\":1}", json);
    }

    @Test
    void toJson_escapesCarriageReturn() {
        String json = TimingResultWriter.toJson("a\rb", 1L);
        assertEquals("{\"fqn\":\"a\\rb\",\"timeNs\":1}", json);
    }

    @Test
    void toJson_escapesTab() {
        String json = TimingResultWriter.toJson("a\tb", 1L);
        assertEquals("{\"fqn\":\"a\\tb\",\"timeNs\":1}", json);
    }

    @Test
    void toJson_escapesControlChar() {
        // U+0001 — произвольный управляющий символ
        String json = TimingResultWriter.toJson("a\u0001b", 1L);
        assertEquals("{\"fqn\":\"a\\u0001b\",\"timeNs\":1}", json);
    }

    @Test
    void toJson_zeroNanos() {
        String json = TimingResultWriter.toJson("com.Foo.m()", 0L);
        assertEquals("{\"fqn\":\"com.Foo.m()\",\"timeNs\":0}", json);
    }

    @Test
    void toJson_largeNanos() {
        String json = TimingResultWriter.toJson("com.Foo.m()", Long.MAX_VALUE);
        assertTrue(json.contains("\"timeNs\":" + Long.MAX_VALUE));
    }

    // ── buildFqn ──────────────────────────────────────────────────────────────

    @Test
    void buildFqn_noParams() throws Exception {
        Method m = SampleClass.class.getDeclaredMethod("noArgs");
        assertEquals("com.github.methodtimer.agent.TimingResultWriterTest.SampleClass.noArgs()", TimingResultWriter.buildFqn(m));
    }

    @Test
    void buildFqn_primitiveAndObjectParams() throws Exception {
        Method m = SampleClass.class.getDeclaredMethod("withArgs", String.class, int.class);
        assertEquals("com.github.methodtimer.agent.TimingResultWriterTest.SampleClass.withArgs(java.lang.String, int)", TimingResultWriter.buildFqn(m));
    }

    @Test
    void buildFqn_arrayParam() throws Exception {
        Method m = SampleClass.class.getDeclaredMethod("withArray", byte[].class);
        assertEquals("com.github.methodtimer.agent.TimingResultWriterTest.SampleClass.withArray(byte[])", TimingResultWriter.buildFqn(m));
    }

    @Test
    void buildFqn_genericParam() throws Exception {
        Method m = SampleClass.class.getDeclaredMethod("withList", List.class);
        // После erasure — java.util.List
        assertEquals("com.github.methodtimer.agent.TimingResultWriterTest.SampleClass.withList(java.util.List)", TimingResultWriter.buildFqn(m));
    }

    // ── helper ────────────────────────────────────────────────────────────────

    @SuppressWarnings("unused")
    static class SampleClass {
        void noArgs() {}
        void withArgs(String s, int i) {}
        void withArray(byte[] b) {}
        void withList(List<String> l) {}
    }
}
