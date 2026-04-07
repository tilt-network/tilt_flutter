package com.example.tilt

import io.flutter.plugin.common.MethodCall
import io.flutter.plugin.common.MethodChannel.Result
import org.mockito.Mockito.mock
import org.mockito.Mockito.verify
import org.mockito.Mockito.never
import org.mockito.ArgumentMatchers.any
import org.mockito.ArgumentMatchers.anyString
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertTrue

/*
 * Unit tests for TiltPlugin method call routing and argument validation.
 *
 * These tests cover the Dart↔Kotlin bridge logic (MethodChannel dispatch,
 * argument parsing, error handling) without invoking the Tilt SDK or
 * Android framework — both are exercised by integration tests.
 *
 * Run with: ./gradlew testDebugUnitTest  (from the flutter plugin's android/ folder)
 */
internal class TiltPluginTest {

    // ── helpers ───────────────────────────────────────────────────────────────

    private fun plugin() = TiltPlugin()
    private fun mockResult() = mock(Result::class.java)

    private fun capturingResult(
        onSuccess: (Any?) -> Unit = {},
        onError: (String, String?, Any?) -> Unit = { _, _, _ -> },
        onNotImplemented: () -> Unit = {},
    ): Result = object : Result {
        override fun success(r: Any?) = onSuccess(r)
        override fun error(code: String, msg: String?, details: Any?) = onError(code, msg, details)
        override fun notImplemented() = onNotImplemented()
    }

    // ── initialize ────────────────────────────────────────────────────────────

    @Test
    fun `initialize - missing publicKey calls result error`() {
        val result = mockResult()
        plugin().onMethodCall(MethodCall("initialize", mapOf("environment" to "staging")), result)
        verify(result).error(anyString(), anyString(), any())
        verify(result, never()).success(any())
    }

    @Test
    fun `initialize - null publicKey calls result error`() {
        val result = mockResult()
        plugin().onMethodCall(
            MethodCall("initialize", mapOf("publicKey" to null, "environment" to "staging")),
            result,
        )
        verify(result).error(anyString(), anyString(), any())
    }

    @Test
    fun `initialize - null args map calls result error`() {
        val result = mockResult()
        plugin().onMethodCall(MethodCall("initialize", null), result)
        verify(result).error(anyString(), anyString(), any())
    }

    @Test
    fun `initialize - error code is MISSING_ARG`() {
        var capturedCode: String? = null
        plugin().onMethodCall(
            MethodCall("initialize", emptyMap<String, Any>()),
            capturingResult(onError = { code, _, _ -> capturedCode = code }),
        )
        assertEquals("MISSING_ARG", capturedCode)
    }

    @Test
    fun `initialize - missing publicKey never calls notImplemented`() {
        val result = mockResult()
        plugin().onMethodCall(MethodCall("initialize", emptyMap<String, Any>()), result)
        verify(result, never()).notImplemented()
    }

    // ── getLogLines ───────────────────────────────────────────────────────────

    @Test
    fun `getLogLines - returns empty ArrayList initially`() {
        var captured: Any? = null
        plugin().onMethodCall(
            MethodCall("getLogLines", null),
            capturingResult(onSuccess = { captured = it }),
        )
        assertTrue(captured is ArrayList<*>, "expected ArrayList, got ${captured?.javaClass}")
        assertTrue((captured as ArrayList<*>).isEmpty())
    }

    @Test
    fun `getLogLines - calls result success`() {
        val result = mockResult()
        plugin().onMethodCall(MethodCall("getLogLines", null), result)
        verify(result).success(any())
        verify(result, never()).error(anyString(), any(), any())
    }

    @Test
    fun `getLogLines - never calls notImplemented`() {
        val result = mockResult()
        plugin().onMethodCall(MethodCall("getLogLines", null), result)
        verify(result, never()).notImplemented()
    }

    // ── unknown method ────────────────────────────────────────────────────────

    @Test
    fun `unknown method calls notImplemented`() {
        val result = mockResult()
        plugin().onMethodCall(MethodCall("doesNotExist", null), result)
        verify(result).notImplemented()
        verify(result, never()).success(any())
        verify(result, never()).error(anyString(), any(), any())
    }

    @Test
    fun `empty method name calls notImplemented`() {
        val result = mockResult()
        plugin().onMethodCall(MethodCall("", null), result)
        verify(result).notImplemented()
    }

    // ── routing completeness ──────────────────────────────────────────────────

    @Test
    fun `recognized methods never call notImplemented`() {
        listOf("initialize", "getLogLines").forEach { method ->
            val result = mockResult()
            plugin().onMethodCall(MethodCall(method, null), result)
            verify(result, never()).notImplemented()
        }
    }
}
