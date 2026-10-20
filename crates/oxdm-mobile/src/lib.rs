use std::sync::OnceLock;
use jni::JNIEnv;
use jni::objects::{JClass, JString};
use jni::sys::jboolean;
use tokio::runtime::Runtime;

static RUNTIME: OnceLock<Runtime> = OnceLock::new();

fn get_runtime() -> &'static Runtime {
    RUNTIME.get_or_init(|| {
        tokio::runtime::Builder::new_multi_thread()
            .worker_threads(4)
            .enable_all()
            .build()
            .expect("failed to initialize tokio runtime for oxdm-mobile")
    })
}

#[unsafe(no_mangle)]
pub extern "system" fn Java_com_oxdm_app_data_OxdmBridge_nativeInit(
    mut env: JNIEnv,
    _class: JClass,
    db_path: JString,
) {
    let db_path: String = match env.get_string(&db_path) {
        Ok(s) => s.into(),
        Err(_) => return,
    };

    let _rt = get_runtime();
    println!("[oxdm-mobile] Initialized with DB directory: {}", db_path);
}

#[unsafe(no_mangle)]
pub extern "system" fn Java_com_oxdm_app_data_OxdmBridge_nativeAddJob(
    mut env: JNIEnv,
    _class: JClass,
    id: JString,
    url: JString,
    filename: JString,
    parts: i32,
) {
    let id: String = match env.get_string(&id) {
        Ok(s) => s.into(),
        Err(_) => return,
    };
    let url: String = match env.get_string(&url) {
        Ok(s) => s.into(),
        Err(_) => return,
    };
    let filename: String = match env.get_string(&filename) {
        Ok(s) => s.into(),
        Err(_) => return,
    };

    let rt = get_runtime();
    rt.spawn(async move {
        println!(
            "[oxdm-mobile] Starting download job: id={}, url={}, filename={}, parts={}",
            id, url, filename, parts
        );
    });
}

#[unsafe(no_mangle)]
pub extern "system" fn Java_com_oxdm_app_data_OxdmBridge_nativePauseJob(
    mut env: JNIEnv,
    _class: JClass,
    id: JString,
) {
    let id: String = match env.get_string(&id) {
        Ok(s) => s.into(),
        Err(_) => return,
    };
    println!("[oxdm-mobile] Pausing job: {}", id);
}

#[unsafe(no_mangle)]
pub extern "system" fn Java_com_oxdm_app_data_OxdmBridge_nativeResumeJob(
    mut env: JNIEnv,
    _class: JClass,
    id: JString,
) {
    let id: String = match env.get_string(&id) {
        Ok(s) => s.into(),
        Err(_) => return,
    };
    println!("[oxdm-mobile] Resuming job: {}", id);
}

#[unsafe(no_mangle)]
pub extern "system" fn Java_com_oxdm_app_data_OxdmBridge_nativeCancelJob(
    mut env: JNIEnv,
    _class: JClass,
    id: JString,
) {
    let id: String = match env.get_string(&id) {
        Ok(s) => s.into(),
        Err(_) => return,
    };
    println!("[oxdm-mobile] Canceling job: {}", id);
}

#[unsafe(no_mangle)]
pub extern "system" fn Java_com_oxdm_app_data_OxdmBridge_nativeDeleteJob(
    mut env: JNIEnv,
    _class: JClass,
    id: JString,
    delete_file: jboolean,
) {
    let id: String = match env.get_string(&id) {
        Ok(s) => s.into(),
        Err(_) => return,
    };
    let delete_file = delete_file != 0;
    println!("[oxdm-mobile] Deleting job: {}, delete_file={}", id, delete_file);
}
