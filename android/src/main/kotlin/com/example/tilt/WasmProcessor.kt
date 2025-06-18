import android.annotation.SuppressLint
import android.content.Context
import android.util.Base64
import androidx.javascriptengine.JavaScriptIsolate
import androidx.javascriptengine.JavaScriptSandbox
import kotlinx.coroutines.guava.await
import java.io.Closeable
import android.util.Log

class WasmProcessor(private val context: Context) : Closeable {
    private val appContext = context.applicationContext

    @SuppressLint("RequiresFeature")
    suspend fun execute(programBytes: ByteArray, dataBytes: ByteArray): ByteArray {
        val sandbox = JavaScriptSandbox.createConnectedInstanceAsync(appContext).await()
        val isolate = sandbox.createIsolate()

        try {
            if (!sandbox.isFeatureSupported(JavaScriptSandbox.JS_FEATURE_PROVIDE_CONSUME_ARRAY_BUFFER)) {
                throw IllegalStateException("JS_FEATURE_PROVIDE_CONSUME_ARRAY_BUFFER não disponível neste runtime.")
            }

            isolate.provideNamedData("programBytes", programBytes)
            isolate.provideNamedData("dataBytes", dataBytes)

            val jsCode = """
                let wasmInstance;
                let memory;
                
                const BASE64_CHARS = "ABCDEFGHIJKLMNOPQRSTUVWXYZabcdefghijklmnopqrstuvwxyz0123456789+/";

                function uint8ArrayToBase64(bytes) {
                    let result = "";
                    let i;
                    const len = bytes.byteLength;
                    for (i = 0; i + 2 < len; i += 3) {
                        const triple = (bytes[i] << 16) | (bytes[i + 1] << 8) | bytes[i + 2];
                        result += BASE64_CHARS[(triple >> 18) & 0x3F];
                        result += BASE64_CHARS[(triple >> 12) & 0x3F];
                        result += BASE64_CHARS[(triple >> 6) & 0x3F];
                        result += BASE64_CHARS[triple & 0x3F];
                    }
                    if (i < len) {
                        let triple = (bytes[i] << 16);
                        let padCount = 2;
                        if (i + 1 < len) {
                            triple |= (bytes[i + 1] << 8);
                            padCount = 1;
                        }
                        result += BASE64_CHARS[(triple >> 18) & 0x3F];
                        result += BASE64_CHARS[(triple >> 12) & 0x3F];
                        result += padCount >= 2 ? "=" : BASE64_CHARS[(triple >> 6) & 0x3F];
                        result += "=";
                    }
                    return result;
                }
                async function initProgram(bytes) {
                    const wasmModule = await WebAssembly.instantiate(bytes, {
                        env: {
                            // 160 pages × 64 KiB = 10 485 760 bytes (~10 MB)
                            memory: new WebAssembly.Memory({
                                initial: 160,
                                maximum: 160,
                            }),
                        },
                    });
                    wasmInstance = wasmModule.instance;
                    memory = wasmInstance.exports.memory;
                }
                
                function getBytes(ptr, len) {
                    return new Uint8Array(memory.buffer, ptr, len).slice();
                }
                
                async function passData(data) {
                    let bytes;
                    if (typeof data === "string") {
                        bytes = new TextEncoder().encode(data);
                    // } else if (data instanceof Blob) {
                    //     bytes = new Uint8Array(await data.arrayBuffer());
                    } else if (data instanceof ArrayBuffer) {
                        bytes = new Uint8Array(data);
                    } else if (data instanceof Uint8Array) {
                        bytes = data;
                    } else {
                        throw new Error("Type not supported");
                    }

                    const ptr = wasmInstance.exports.alloc(bytes.length);
                    new Uint8Array(memory.buffer, ptr, bytes.length).set(bytes);
                    return { ptr, len: bytes.length };
                }
                
                async function executeProgram(data) {
                    const { ptr, len } = await passData(data);
                    const retptr = wasmInstance.exports.alloc(8);

                    wasmInstance.exports.execute(retptr, ptr, len);

                    const resultView = new Uint32Array(memory.buffer, retptr, 2);
                    const result_ptr = resultView[0];
                    const result_len = resultView[1];

                    const output = getBytes(result_ptr, result_len);

                    wasmInstance.exports.free_buffer(ptr, len);
                    wasmInstance.exports.free_buffer(result_ptr, result_len);
                    wasmInstance.exports.free_buffer(retptr, 8);

                    return output;
                }
                
                (async function() {
                    const [progBuf, dataBuf] = await Promise.all([
                        android.consumeNamedDataAsArrayBuffer('programBytes'),
                        android.consumeNamedDataAsArrayBuffer('dataBytes')
                    ])
                    const progArr = new Uint8Array(progBuf);
                    const dataArr = new Uint8Array(dataBuf);
                    await initProgram(progArr);
                    
                    const result = await executeProgram(dataArr);
                    
                    return uint8ArrayToBase64(result)
                })();
            """

            val resultBase64 = isolate.evaluateJavaScriptAsync(jsCode).await()
            val outputBytes = Base64.decode(resultBase64, Base64.DEFAULT)
            Log.i("WasmProcessor", "Result has ${outputBytes.size} bytes")
            return outputBytes
        } catch (e: Exception) {
            Log.e("WasmProcessor", "Error executing WASM: ${e.message}")
            throw e
        } finally {
            isolate.close()
            sandbox.close()
        }
    }

    override fun close() {
    }
}
