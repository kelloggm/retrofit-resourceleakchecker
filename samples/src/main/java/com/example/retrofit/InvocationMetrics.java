/*
 * Copyright (C) 2018 Square, Inc.
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *      http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package com.example.retrofit;

import java.io.IOException;
import okhttp3.Interceptor;
import okhttp3.OkHttpClient;
import okhttp3.Request;
import okhttp3.Response;
import okhttp3.ResponseBody;
import retrofit2.Call;
import retrofit2.Invocation;
import retrofit2.Retrofit;
import retrofit2.http.GET;
import retrofit2.http.Url;
import org.checkerframework.checker.mustcall.qual.*;
import org.checkerframework.checker.calledmethods.qual.*;
import org.checkerframework.dataflow.qual.SideEffectFree;
import org.checkerframework.common.returnsreceiver.qual.This;
import org.checkerframework.framework.qual.*;

/** This example prints HTTP call metrics with the initiating method names and arguments. */
public final class InvocationMetrics {
  public interface Browse {
    @GET("/robots.txt")
    Call<ResponseBody> robots();

    @GET("/favicon.ico")
    Call<ResponseBody> favicon();

    @GET("/")
    Call<ResponseBody> home();

    @GET
    Call<ResponseBody> page(@Url String path);
  }

  static final class InvocationLogger implements Interceptor {
    @Override
    public Response intercept(Chain chain) throws IOException {
      Request request = chain.request();
      long startNanos = System.nanoTime();
      Response response = chain.proceed(request);
      long elapsedNanos = System.nanoTime() - startNanos;

      Invocation invocation = request.tag(Invocation.class);
      if (invocation != null) {
        System.out.printf(
            "%s.%s %s HTTP %s (%.0f ms)%n",
            invocation.method().getDeclaringClass().getSimpleName(),
            invocation.method().getName(),
            invocation.arguments(),
            response.code(),
            elapsedNanos / 1_000_000.0);
      }

      return response;
    }
  }
  @SuppressWarnings("calledmethods:required.method.not.called")
  public static void main(String... args) throws IOException {
    InvocationLogger invocationLogger = new InvocationLogger();

    OkHttpClient okHttpClient = new OkHttpClient.Builder().addInterceptor(invocationLogger).build();

    Retrofit retrofit =
        new Retrofit.Builder().baseUrl("https://square.com/").callFactory(okHttpClient).build();

    Browse browse = retrofit.create(Browse.class);
    /*
     * Three methods below: A resource leak is possible is an exception is thrown from execute.
     */
    browse.robots().execute();
    browse.favicon().execute();
    browse.home().execute();
    /*
     * Two methods below: It seems like the resources are not closed? If so then this is a resource leak. further investigation is needed to confirm this due to the type being retrofit2.Response<okhttp3.ResponseBody>.
     */
    browse.page("sitemap.xml").execute();
    browse.page("notfound").execute();
  }
}
