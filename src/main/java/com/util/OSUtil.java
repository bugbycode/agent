package com.util;

import java.util.Properties;

public class OSUtil {
	public static Properties props = System.getProperties(); //获得系统属性集   
	
	/**
	 * 操作系统名称
	 * @return String
	 */
	public static String getOSName() {
		return OSUtil.getProperty("os.name");
	}
	
	/**
	 * 操作系统架构
	 * @return String
	 */
	public static String getOSArch() {
		return OSUtil.getProperty("os.arch");
	}
	
	/**
	 * 操作系统版本
	 * @return String
	 */
	public static String getOSVersion() {
		return OSUtil.getProperty("os.version");
	}
	
	/**
	 * Java 运行时环境版本 
	 * @return String
	 */
	public static String getJavaVersion() {
		return OSUtil.getProperty("java.version");
	}
	
	/**
	 * Java 运行时环境供应商
	 * @return String
	 */
	public static String getJavaVendor() {
		return OSUtil.getProperty("java.vendor");
	}
	
	/**
	 * Java 供应商的 URL 
	 * @return String
	 */
	public static String getJavaVendorURL() {
		return OSUtil.getProperty("java.vendor.url");
	}
	
	/**
	 * Java 安装目录
	 * @return String
	 */
	public static String getJavaHome() {
		return OSUtil.getProperty("java.home");
	}
	
	/**
	 * Java 虚拟机规范版本 
	 * @return String
	 */
	public static String getJavaVmSpecificationVersion() {
		return OSUtil.getProperty("java.vm.specification.version");
	}
	
	/**
	 * Java 虚拟机规范供应商  
	 * @return String
	 */
	public static String getJavaVmSpecificationVendor() {
		return OSUtil.getProperty("java.vm.specification.vendor");
	}
	
	/**
	 * Java 虚拟机规范名称  
	 * @return String
	 */
	public static String getJavaVmSpecificationName() {
		return OSUtil.getProperty("java.vm.specification.name");
	}
	
	/**
	 * Java 虚拟机实现版本 
	 * @return String
	 */
	public static String getJavaVmVersion() {
		return OSUtil.getProperty("java.vm.version");
	}
	
	/**
	 * Java 虚拟机实现供应商  
	 * @return String
	 */
	public static String getJavaVmVendor() {
		return OSUtil.getProperty("java.vm.vendor");
	}
	
	/**
	 * Java 虚拟机实现名称   
	 * @return String
	 */
	public static String getJavaVmName() {
		return OSUtil.getProperty("java.vm.name");
	}
	
	/**
	 * Java 运行时环境规范版本 
	 * @return String
	 */
	public static String getJavaSpecificationVersion() {
		return OSUtil.getProperty("java.specification.version");
	}
	
	/**
	 * Java 运行时环境规范供应商 
	 * @return String
	 */
	public static String getJavaSpecificationVendor() {
		return OSUtil.getProperty("java.specification.vendor");
	}
	
	/**
	 * Java 运行时环境规范名称 
	 * @return String
	 */
	public static String getJavaSpecificationName() {
		return OSUtil.getProperty("java.specification.name");
	}
	
	/**
	 * Java 类格式版本号  
	 * @return String
	 */
	public static String getJavaClassVersion() {
		return OSUtil.getProperty("java.class.version");
	}
	
	/**
	 * Java 类路径   
	 * @return String
	 */
	public static String getJavaClassPath() {
		return OSUtil.getProperty("java.class.path");
	}
	
	/**
	 * 加载库时搜索的路径列表  
	 * @return String
	 */
	public static String getJavaLibraryPath() {
		return OSUtil.getProperty("java.library.path");
	}
	
	/**
	 * 默认的临时文件路径 
	 * @return String
	 */
	public static String getJavaIOTmpdir() {
		return OSUtil.getProperty("java.io.tmpdir");
	}
	
	/**
	 * 要使用的 JIT 编译器的名称
	 * @return String
	 */
	public static String getJavaCompiler() {
		return OSUtil.getProperty("java.compiler");
	}
	
	/**
	 * 一个或多个扩展目录的路径 
	 * @return String
	 */
	public static String getJavaExtDirs() {
		return OSUtil.getProperty("java.ext.dirs");
	}
	
	/**
	 * 文件分隔符（在 UNIX 系统中是“/”）
	 * @return String
	 */
	public static String getFileSeparator() {
		return OSUtil.getProperty("file.separator");
	}
	
	/**
	 * 路径分隔符（在 UNIX 系统中是“:”） 
	 * @return String
	 */
	public static String getPathSeparator() {
		return OSUtil.getProperty("path.separator");
	}
	
	/**
	 * 行分隔符（在 UNIX 系统中是“:”） 
	 * @return String
	 */
	public static String getLineSeparator() {
		return OSUtil.getProperty("line.separator");
	}
	
	/**
	 * 用户的账户名称 
	 * @return String
	 */
	public static String getUserName() {
		return OSUtil.getProperty("user.name");
	}
	
	/**
	 * 用户的主目录 
	 * @return String
	 */
	public static String getUserHome() {
		return OSUtil.getProperty("user.home");
	}
	
	/**
	 * 用户的当前工作目录 
	 * @return String
	 */
	public static String getUserDir() {
		return OSUtil.getProperty("user.dir");
	}
	
	private static String getProperty(String key) {
		return props.getProperty(key);
	}
	
	/**
	 * @param args
	 */
	public static void main(String[] args) {
		System.out.println(OSUtil.getOSName());
	}

}
