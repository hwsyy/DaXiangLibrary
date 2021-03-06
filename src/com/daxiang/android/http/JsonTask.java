package com.daxiang.android.http;

import java.util.List;

import org.apache.http.NameValuePair;

import android.content.Context;
import android.os.Handler;
import android.os.Message;
import android.text.TextUtils;

import com.daxiang.android.http.HttpConstants.HttpMethod;
import com.daxiang.android.util.CharToUrlTools;
import com.daxiang.android.util.Logger;

/**
 * 
 * @author daxiang
 * 
 *         2015-3-24
 */
public class JsonTask implements Runnable {
	private static final String TAG = JsonTask.class.getSimpleName();
	private Handler handler;
	private String path;
	private int requestCode;
	private boolean isCancel;

	/**
	 * 直接从网络取数据不需要本地存储 DATA_FROM_NET_NO_CACHE
	 * 
	 * 直接从网络取数据需要本地存储 DATA_FROM_NET_AND_CACHE
	 * 
	 * 直接从本地存储拿数据 DATA_FROM_CACHE
	 * 
	 * 先从本地存储取数据显示出来 再去网络取数据更新界面并本地存储 DATA_FROM_CACHE_THEN_NET
	 */
	private int dataAccessMode = HttpConstants.NetDataProtocol.DATA_FROM_NET_NO_CACHE;
	/**
	 * 为了区别不同的请求，当页面有多个请求时需要改变此变量区别
	 */
	private int dataSuccess = HttpConstants.NetDataProtocol.LOAD_SUCCESS;
	private Context mContext;
	private HttpMethod method = HttpConstants.HttpMethod.GET;
	private List<NameValuePair> postParameters = null;

	/**
	 * 
	 * @param context
	 * @param handler
	 * @param path
	 * @param requestCode
	 * @param method
	 * @param postParameters
	 */

	public JsonTask(Context context, Handler handler, String path,
			int requestCode, HttpMethod method,
			List<NameValuePair> postParameters) {
		this.handler = handler;
		this.path = CharToUrlTools.toUtf8String(path);
		this.requestCode = requestCode;
		this.mContext = context;
		this.method = method;
		this.postParameters = postParameters;
	}

	public JsonTask setDataAccessMode(int dataAccessMode) {
		this.dataAccessMode = dataAccessMode;
		return this;
	}

	@Override
	public void run() {
		Message msg = handler.obtainMessage();
		msg.arg1 = requestCode;
		String json = null;
		try {
			if (isCancel) {
				Logger.i(TAG, "cancel this task before run!");
				return;
			}
			switch (dataAccessMode) {
			// 访问网络，不做本地存储
			case HttpConstants.NetDataProtocol.DATA_FROM_NET_NO_CACHE:
				json = JsonUtil.getJsonFromServer(path, mContext, method,
						postParameters);
				break;

			case HttpConstants.NetDataProtocol.DATA_FROM_NET_AND_CACHE:
				json = JsonUtil.getJsonFromServer(path, true, mContext, method,
						postParameters);
				break;

			// 仅访问本地存储
			case HttpConstants.NetDataProtocol.DATA_FROM_CACHE:
				json = JsonUtil.getJsonFromFile(path, mContext);
				break;

			// 先访问本地存储返回数据展示，再访问网络更新数据并刷新UI
			case HttpConstants.NetDataProtocol.DATA_FROM_CACHE_THEN_NET:
				json = JsonUtil.getJsonFromFile(path, mContext);
				if (!TextUtils.isEmpty(json)) {
					Message msg1 = handler.obtainMessage();
					msg1.arg1 = requestCode;
					msg1.what = dataSuccess;
					msg1.obj = json;
					handlerMessage(msg1);
				}
				json = JsonUtil.getJsonFromServer(path, true, mContext, method,
						postParameters);
				break;

			// 默认仅访问网络，并且不做本地缓存；
			default:
				json = JsonUtil.getJsonFromServer(path, mContext, method,
						postParameters);
				break;
			}

			if (TextUtils.isEmpty(json)) {
				msg.what = HttpConstants.NetDataProtocol.LOAD_MISTAKE;
				handlerMessage(msg);
			} else {
				msg.what = dataSuccess;
				msg.obj = json;
				handlerMessage(msg);
			}

		} catch (Exception e) {
			msg.what = HttpConstants.NetDataProtocol.LOAD_MISTAKE;
			handlerMessage(msg);
			e.printStackTrace();
		}
	}

	private void handlerMessage(Message msg) {
		if (isCancel) {
			Logger.i(TAG, "cancel this task after run!");
			return;
		}
		Logger.i(TAG, "handlerMessage callback");
		handler.sendMessage(msg);
	}

	public void cancel() {
		isCancel = true;
	}

}