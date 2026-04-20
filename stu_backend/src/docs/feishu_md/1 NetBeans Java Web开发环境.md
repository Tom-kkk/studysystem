
## **一 主要内容**
1.了解JDK、NetBeans和Tomcat；
2.掌握Java Web开发环境的搭建；
3.掌握Java Web项目的建立；
4.掌握编写Servlet和JSP程序的基本步骤。
## **二 基础知识**
1.** JDK：**Java Development Kit 是Java的标准开发工具包（普通用户只需要安装 JRE来运行 Java 程序。而程序开发者必须安装JDK来编译、调试程序）。它提供了编译、运行Java程序所需的各种工具和资源，包括Java编译器、Java运行环境JRE，以及常用的Java基础类库等，是JAVA的核心。
**JRE：**Java runtime environment 是运行基于Java语言编写的程序所不可缺少的运行环境，用于解释执行Java的字节码文件。
**JVM****：**Java Virtual Machine 是Java的虚拟机，是JRE的一部分。JVM是整个java实现跨平台的最核心的部分，负责解释执行字节码文件，是可运行java字节码文件的虚拟计算机。
JDK用于开发，是给开发人员用的；JRE 用于运行java程序，和JVM是给普通用户使用的。如果只是运行Java程序，可以只安装JRE，无需安装JDK。使用JDK开发完成的java程序，交给JRE去运行。JDK包含了JRE，JDK 和 JRE 中都包含 JVM。JDK是JRE+Java的开发工具。JRE包含了JVM+Java语言的核心类库。
2. Tomcat 服务器是一个免费的开放源代码的Web 应用服务器。Tomcat属于轻量级应用[服务器](http://baike.baidu.com/view/899.htm)，是开发和调试Java Web程序的首选服务器。
3. NetBeans IDE是一个免费、开源集成开发环境，NetBeans IDE可以使开发人员利用Java平台快速创建Web、企业、桌面以及移动的应用程序，NetBeans IDE已经支持PHP、Ruby、JavaScript、Groovy、Grails和C/C++等开发语言。
## **三 NetBeans Web应用**
### **1 下载**
*JDK下载网址：https://www.oracle.com/java/technologies/downloads/archive/
本例中使用的是JDK 8
![](assets/01-1 NetBeans Java Web开发环境-img-0.png)

![](assets/01-1 NetBeans Java Web开发环境-img-1.png)


安装过程中使用默认设置即可。
*Tomcat网址：[https://tomcat.apache.org/](https://tomcat.apache.org/)，本案例中使用的是Tomcat9，下载[64-bit Windows zip](https%3A%2F%2Fdlcdn.apache.org%2Ftomcat%2Ftomcat-9%2Fv9.0.73%2Fbin%2Fapache-tomcat-9.0.73-windows-x64.zip)解压即可。
![](assets/01-1 NetBeans Java Web开发环境-img-2.png)

*NetBeans网址：[https://netbeans.apache.org/](https://netbeans.apache.org/)
NetBeans IDE是一个免费、开源集成开发环境，NetBeans IDE可以使开发人员利用Java平台快速创建Web、企业、桌面以及移动的应用程序，NetBeans IDE已经支持PHP、Ruby、JavaScript、Groovy、Grails和C/C++等开发语言。
NetBeans最开始由Sun开发的，2009年Sun被Oracle收购后就归于Oracle旗下。后来Oracle将NetBeans捐给了Apache基金会。
在准备此文档时，使用的版本是Apache NetBeans 14（发布于June 9,2022）
![](assets/01-1 NetBeans Java Web开发环境-img-3.png)


### **2.安装NetBeans**
 (1)安装过程中选择JDK
![](assets/01-1 NetBeans Java Web开发环境-img-4.png)

安装完成后运行NetBeans，新建一个Java应用项目（测试目的），点击File->New Project
![](assets/01-1 NetBeans Java Web开发环境-img-5.png)

设置项目名称和位置（Name and Location），
![](assets/01-1 NetBeans Java Web开发环境-img-6.png)

完成（Finish）。
操作如下图提示，编辑代码，然后运行项目，
![](assets/01-1 NetBeans Java Web开发环境-img-7.png)

运行结果如下图
![](assets/01-1 NetBeans Java Web开发环境-img-8.png)

### **3.建立Java Web应用项目**
（1）File->New project新建项目**：**  
![](assets/01-1 NetBeans Java Web开发环境-img-9.png)

（2）**名称与位置：**
![](assets/01-1 NetBeans Java Web开发环境-img-10.png)


（3）**服务器与设置：** 
  {align="center"}
![](assets/01-1 NetBeans Java Web开发环境-img-11.png)


![](assets/01-1 NetBeans Java Web开发环境-img-12.png)


![](assets/01-1 NetBeans Java Web开发环境-img-13.png)

![](assets/01-1 NetBeans Java Web开发环境-img-14.png)


![](assets/01-1 NetBeans Java Web开发环境-img-15.png)

![](assets/01-1 NetBeans Java Web开发环境-img-16.png)

### **4.一个简单的JSP程序**
（1）在项目视窗中右击前面新建的项目，
![](assets/01-1 NetBeans Java Web开发环境-img-17.png)

选择“New”->“JSP”，
![](assets/01-1 NetBeans Java Web开发环境-img-18.png)

设置文件名，单击完成。
![](assets/01-1 NetBeans Java Web开发环境-img-19.png)

（2）编辑新建的JSP程序：test_jsp
```java
<%-- 
    程序: test_jsp 
--%>
<%@page contentType="text/html" pageEncoding="UTF-8"%>
<!DOCTYPE html>
<html> 
    <head>
        <meta http-equiv="Content-Type" content="text/html; charset=UTF-8">
        <title>登录页面</title>
    </head>
    <body>
        <form action=" TestServlet" method="post">
            <table>
            <tr><td>用户名：</td>
                <td><input type="text" name="username"/></td>
            </tr>
            <tr><td>密&nbsp;&nbsp;码：</td>
                <td><input type="password" name="password"/></td>
            </tr>
            <tr><td><input type="submit" value="登录"/></td>
                <td><input type="reset" value="重设"/></td>
            </tr>
            </table>
        </form>
    </body>
</html>
```

<grid cols="3">
  <column width="33">
    （3）运行项目，在“项目”窗口中右击项目
  </column>
  <column width="33">
    ![](assets/01-1 NetBeans Java Web开发环境-img-20.png)

    
  </column>
  <column width="33">
    
  </column>
</grid>

![](assets/01-1 NetBeans Java Web开发环境-img-21.png)

也可以点击工具按钮运行项目
![](assets/01-1 NetBeans Java Web开发环境-img-22.png)

在浏览器地址栏后面输入:test_jsp.jsp,
![](assets/01-1 NetBeans Java Web开发环境-img-23.png)

按回车查看运行结果。
<callout emoji="art" background-color="light-orange" border-color="light-orange">
注：关于Netbeans的 Tomcat 控制台输出中文乱码问题：
a.（此步骤通常可以省略）代码System.out.println(System.getProperty("file.encoding"));显示系统使用的编码，中国的电脑一般是：GBK。
b.修改netbeans安装目录下etc文件夹内netbeans.conf文件，找到netbeans_default_options选项，在双引号内，要在两个冒号（:）之间找位置加入：-J-Dfile.encoding=GBK
c.找到tomcat目录，conf下有个logging.properties文件，将其中的 UTF-8 全部修改为GBK
</callout>


### **5.一个简单的Servlet程序**
（1）在“项目”窗口中右击项目
![](assets/01-1 NetBeans Java Web开发环境-img-24.png)

选择New->Servlet，设置“类名”和“包名”(包名可以为空，但是不建议创建一个空包)，单击“完成”，建立Servlet程序“TestServlet”。
![](assets/01-1 NetBeans Java Web开发环境-img-25.png)


完成后，在新建的Servlet程序“TestServlet”代码编辑区插入下图方框的代码，然后**保存**（点击工具保存按钮或者快捷键ctrl+s）**。**
![](assets/01-1 NetBeans Java Web开发环境-img-26.png)


打开前面JSP程序“test_jsp”的结果界面，输入用户名和密码（任意值），然后提交，
![](assets/01-1 NetBeans Java Web开发环境-img-27.png)

TestServlet处理请求并产生响应（结果显示输入的用户名和密码）。
## 四  建立“实验报告提交系统”项目
新建一个Java Web命名项目名称为“WebApp_report” 
![](assets/01-1 NetBeans Java Web开发环境-img-28.png)

![](assets/01-1 NetBeans Java Web开发环境-img-29.png)


->Finish 完成
![](assets/01-1 NetBeans Java Web开发环境-img-30.png)

建立部署描述(DD)文件“web.xml”
![](assets/01-1 NetBeans Java Web开发环境-img-31.png)


![](assets/01-1 NetBeans Java Web开发环境-img-32.png)

->Next，然后Finish，如下图。
![](assets/01-1 NetBeans Java Web开发环境-img-33.png)

![](assets/01-1 NetBeans Java Web开发环境-img-34.png)

插入代码，设置welcom-file-list
```javascript
    <welcome-file-list>
        <welcome-file>login.jsp</welcome-file>
    </welcome-file-list>
```

![](assets/01-1 NetBeans Java Web开发环境-img-35.png)

建立登录界面JSP程序“login.jsp”
![](assets/01-1 NetBeans Java Web开发环境-img-36.png)

设置文件名，然后Finish。
![](assets/01-1 NetBeans Java Web开发环境-img-37.png)

对应文件结构如下图。
![](assets/01-1 NetBeans Java Web开发环境-img-38.png)

回到IDE，编辑“login.jsp”
```html {wrap}
<%-- 
    Document   : login
    Created on : 2022年10月31日, 下午4:58:43
    Author     : cyl
--%>

<%@page contentType="text/html" pageEncoding="UTF-8"%>
<!DOCTYPE html> 
<html> 
  <head>
    <meta http-equiv="Content-Type" content="text/html; charset=UTF-8">
    <title>登录</title>
  </head>   
  <body>      
  <div style="width:500px;height:300px;position:absolute;top:10%;left:30%;">
    <form action="LoginServlet" method="post" >
      <!-- form属性action="LoginServlet"中，LoginServlet待后续实现，用于处理请求--> 
      <table>
        <tr>
          <td>用户名</td>
          <td><input style="width: 150px;height:30px;" type="text" name="username" size="20"></td>
        </tr>
        <tr>
          <td>密&nbsp;&nbsp;码</td>
          <td><input style="width: 150px;height:30px;" type="password" name="password" size="20"></td>
        </tr>
        <tr>
          <td>验证码</td>
          <td><input style="width: 100px;height:30px;" type="text" name="checkcode" size="15">
            <img style="width:150px;height:32px;" id="img" src="CheckcodeServlet"  alt="[验证码待续]" size="10" align="center">
            <!-- id为img的页面元素,显示验证码图像；src="CheckcodeServlet"中，CheckcodeServlet待后续实现-->                    
          </td>
        </tr>  
        <tr><td></td>                    
          <td><input size-="25" style="margin-left:40px;font-size:20px;" type="submit" value="登&nbsp;录">
            <font style="font-size:15px">&nbsp;&nbsp;&nbsp;&nbsp;<a href="forgetPassword.jsp">忘记密码 </a> </font></td>                                   
        </tr>
      </table>
    </form>
        <div>${requestScope.login_error}</div>
        <div>${requestScope.checkcode_error}</div>
  </div>
</body>
</html>
```

代码16行，form属性action="LoginServlet"中，Servlet程序“LoginServlet”待后续实现，用于处理请求；代码16行，id为img的页面元素,显示验证码图像，其属性src="CheckcodeServlet"中，Servlet程序“CheckcodeServlet”待后续实现。

