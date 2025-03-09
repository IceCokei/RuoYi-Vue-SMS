<template>
  <div class="login">
    <el-form ref="loginForm" :model="loginForm" :rules="loginRules" class="login-form">
      <h3 class="title">若依后台管理系统</h3>
      <el-form-item prop="username" v-if="!isSmsLogin">
        <el-input v-model="loginForm.username" type="text" auto-complete="off" placeholder="账号">
          <svg-icon slot="prefix" icon-class="user" class="el-input__icon input-icon" />
        </el-input>
      </el-form-item>
      <el-form-item prop="password" v-if="!isSmsLogin">
        <el-input v-model="loginForm.password" type="password" auto-complete="off" placeholder="密码"
          @keyup.enter.native="handleLogin">
          <svg-icon slot="prefix" icon-class="password" class="el-input__icon input-icon" />
        </el-input>
      </el-form-item>
      <el-form-item prop="code" v-if="!isSmsLogin">
        <el-input v-model="loginForm.code" auto-complete="off" placeholder="验证码" style="width: 63%"
          @keyup.enter.native="handleLogin">
          <svg-icon slot="prefix" icon-class="validCode" class="el-input__icon input-icon" />
        </el-input>
        <div class="login-code">
          <img :src="codeUrl" @click="getCode" class="login-code-img" />
        </div>
      </el-form-item>

      <el-form-item prop="mobile" v-if="isSmsLogin">
        <el-input v-model="smsLoginForm.mobile" type="text" auto-complete="off" placeholder="手机号">
          <svg-icon slot="prefix" icon-class="user" class="el-input__icon input-icon" />
        </el-input>
      </el-form-item>
      <el-form-item prop="smsCode" v-if="isSmsLogin">
        <el-input v-model="smsLoginForm.smsCode" auto-complete="off" placeholder="验证码" style="width: 63%"
          @keyup.enter.native="handleSmsLogin">
          <svg-icon slot="prefix" icon-class="validCode" class="el-input__icon input-icon" />
        </el-input>
        <div class="login-code">
          <el-button round @click.native.prevent="getSmsCode" :disabled="smsCodeBtnDisabled">{{ smsCodeBtnText
          }}</el-button>
        </div>
      </el-form-item>

      <el-row>
        <el-checkbox v-model="loginForm.rememberMe" style="margin:0px 0px 25px 0px;">{{ isSmsLogin ? '记住手机号' :
          '记住密码' }}</el-checkbox>
        <div class="sms-login">
          <el-button size="mini" type="text" @click.native.prevent="toggleSmsLogin">
            <span v-if="isSmsLogin">账号密码登录</span>
            <span v-else>短信登录</span>
          </el-button>
        </div>
      </el-row>

      <el-form-item style="width:100%;">
        <el-button :loading="loading" size="medium" type="primary" style="width:100%;"
          @click.native.prevent="isSmsLogin ? handleSmsLogin() : handleLogin()">
          <span v-if="!loading">登 录</span>
          <span v-else>登 录 中...</span>
        </el-button>
      </el-form-item>
    </el-form>
    <!--  底部  -->
    <div class="el-login-footer">
      <span>Copyright © 2018-2020 ruoyi.vip All Rights Reserved.</span>
    </div>
  </div>
</template>

<script>
import { getCodeImg, login, smsLogin, getSmsCode, simpleSmsLogin } from "@/api/login";
import Cookies from "js-cookie";
import { encrypt, decrypt } from '@/utils/jsencrypt'
import { setToken } from '@/utils/auth'

export default {
  name: "Login",
  data() {
    return {
      codeUrl: "",
      cookiePassword: "",
      computeTime: 0, // 使用computeTime代替复杂的倒计时逻辑
      loginForm: {
        username: "",
        password: "",
        rememberMe: false,
        code: "",
        uuid: ""
      },
      smsLoginForm: {
        mobile: "",
        smsCode: "",
        uuid: "",
        rememberMe: false
      },
      loginRules: {
        username: [
          { required: true, trigger: "blur", message: "请输入您的账号" }
        ],
        password: [
          { required: true, trigger: "blur", message: "请输入您的密码" }
        ],
        code: [{ required: true, trigger: "change", message: "请输入验证码" }]
      },
      loading: false,
      // 验证码组件控制
      captchaEnabled: true,
      // 短信验证码按钮状态
      smsCodeBtnDisabled: false,
      smsCodeBtnText: "获取验证码",
      smsCodeTimer: null,
      smsCodeCountDown: 60,
      // 登录类型：account为账号密码，mobile为短信登录
      isSmsLogin: false,
      // 记住手机号
      rememberMobile: false,
      redirect: undefined
    };
  },
  watch: {
    $route: {
      handler: function (route) {
        this.redirect = route.query && route.query.redirect;
      },
      immediate: true
    }
  },
  created() {
    this.getCode();
    this.getCookie();
    // 初始化时同步UUID
    this.smsLoginForm.uuid = this.loginForm.uuid;
  },
  methods: {
    getCode() {
      getCodeImg().then(res => {
        this.captchaEnabled = res.captchaEnabled === undefined ? true : res.captchaEnabled;
        if (this.captchaEnabled) {
          this.codeUrl = "data:image/gif;base64," + res.img;
          this.loginForm.uuid = res.uuid;
          this.smsLoginForm.uuid = res.uuid; // 同步UUID到短信登录表单
        }
      });
    },
    getCookie() {
      const username = Cookies.get("username");
      const password = Cookies.get("password");
      const rememberMe = Cookies.get('rememberMe');
      const mobile = Cookies.get("mobile");

      this.loginForm = {
        username: username === undefined ? this.loginForm.username : username,
        password: password === undefined ? this.loginForm.password : decrypt(password),
        rememberMe: rememberMe === undefined ? false : Boolean(rememberMe),
        code: this.loginForm.code,
        uuid: this.loginForm.uuid
      };

      if (mobile) {
        this.smsLoginForm.mobile = mobile;
        this.smsLoginForm.rememberMe = Boolean(rememberMe);
      }
    },

    // 切换登录方式
    toggleSmsLogin() {
      this.isSmsLogin = !this.isSmsLogin;
      // 切换时确保短信验证表单获得最新的UUID
      this.smsLoginForm.uuid = this.loginForm.uuid;
    },

    // 获取短信验证码
    getSmsCode() {
      if (this.smsCodeBtnDisabled) {
        return;
      }

      if (!this.smsLoginForm.mobile) {
        this.$message.error('请输入手机号码');
        return;
      }

      // 手机号格式验证
      if (!/^1[3-9]\d{9}$/.test(this.smsLoginForm.mobile)) {
        this.$message.error('请输入正确的手机号码');
        return;
      }

      // 显示加载状态
      this.smsCodeBtnDisabled = true;
      this.smsCodeBtnText = '发送中...';

      console.log("发送请求到:", '/system/sms/code?mobile=' + this.smsLoginForm.mobile);

      // 调用短信验证码发送接口
      getSmsCode(this.smsLoginForm.mobile).then(res => {
        console.log("短信验证码响应:", res);

        if (res.code === 200) {
          this.$message.success('验证码发送成功，请查收');
          // 保存uuid，用于登录验证
          this.smsLoginForm.uuid = res.uuid;

          // 启动倒计时
          this.computeTime = 60;
          this.smsCodeBtnText = `(${this.computeTime}s)已发送`;

          this.timer = setInterval(() => {
            this.computeTime--;
            this.smsCodeBtnText = `(${this.computeTime}s)已发送`;

            if (this.computeTime <= 0) {
              clearInterval(this.timer);
              this.smsCodeBtnDisabled = false;
              this.smsCodeBtnText = '获取验证码';
            }
          }, 1000);
        } else {
          // 更好的错误处理
          let errorMsg = '验证码发送失败';
          if (res.msg && typeof res.msg === 'string') {
            errorMsg = res.msg; // 直接显示服务器返回的错误消息
          }
          this.$message.error(errorMsg);
          this.smsCodeBtnDisabled = false;
          this.smsCodeBtnText = '获取验证码';
        }
      }).catch(err => {
        console.error('发送短信验证码出错:', err);
        this.$message.error('验证码发送失败，请稍后重试');
        this.smsCodeBtnDisabled = false;
        this.smsCodeBtnText = '获取验证码';
      });
    },

    // 处理登录请求
    handleLogin() {
      if (this.isSmsLogin) {
        this.handleSmsLogin();
      } else {
        this.handleAccountLogin();
      }
    },

    // 账号密码登录
    handleAccountLogin() {
      this.$refs.loginForm.validate(valid => {
        if (valid) {
          this.loading = true;

          if (this.loginForm.rememberMe) {
            Cookies.set("username", this.loginForm.username, { expires: 30 });
            Cookies.set("password", encrypt(this.loginForm.password), { expires: 30 });
            Cookies.set('rememberMe', this.loginForm.rememberMe, { expires: 30 });
          } else {
            Cookies.remove("username");
            Cookies.remove("password");
            Cookies.remove('rememberMe');
          }

          console.log("准备登录，用户名:", this.loginForm.username);

          // 使用Vuex action处理登录
          this.$store.dispatch("Login", this.loginForm).then(() => {
            let redirect = this.redirect;
            if (!redirect || redirect === "/login") {
              redirect = "/index";
            }
            
            this.$router.replace({ path: redirect }).catch(err => {
              console.error("路由跳转失败:", err);
              if (err.name !== 'NavigationDuplicated') {
                window.location.href = process.env.VUE_APP_BASE_URL + redirect;
              }
            });
          }).catch(() => {
            this.loading = false;
            this.getCode();
          });
        }
      });
    },

    // 简化版短信登录处理
    handleSmsLogin() {
      // 基本验证
      if (!this.smsLoginForm.mobile) {
        this.$message.error("请输入手机号码");
        return;
      }

      if (!this.smsLoginForm.smsCode) {
        this.$message.error("请输入验证码");
        return;
      }

      this.loading = true;
      console.log("开始短信登录验证...");

      // 记住手机号
      if (this.smsLoginForm.rememberMe) {
        Cookies.set("mobile", this.smsLoginForm.mobile, { expires: 30 });
        Cookies.set("rememberMobile", this.smsLoginForm.rememberMe, { expires: 30 });
      } else {
        Cookies.remove("mobile");
        Cookies.remove("rememberMobile");
      }

      // 使用简化版登录API
      simpleSmsLogin(this.smsLoginForm.mobile, this.smsLoginForm.smsCode)
        .then(res => {
          console.log("登录结果:", res);
          if (res.code === 200) {
            // 保存token
            setToken(res.token);
            this.$store.commit('SET_TOKEN', res.token);

            // 修改这部分代码，使用replace而不是push
            let redirect = this.redirect;
            if (!redirect || redirect === "/login") {
              redirect = "/index";
            }
            
            // 使用replace防止导航历史堆栈问题
            this.$router.replace({ path: redirect }).catch(err => {
              console.error("路由跳转失败:", err);
              // 如果路由跳转失败，使用window.location作为备选方案
              if (err.name !== 'NavigationDuplicated') {
                window.location.href = process.env.VUE_APP_BASE_URL + redirect;
              }
            });

            this.loading = false;
            this.$message.success(res.msg || "登录成功");
          } else {
            this.$message.error(res.msg || "验证码错误");
            this.loading = false;
          }
        })
        .catch(error => {
          console.error("登录失败:", error);
          let errorMessage = "登录失败，请稍后重试";

          // Try to extract useful error message
          if (error.response && error.response.data) {
            errorMessage = error.response.data.msg || errorMessage;
          } else if (typeof error === 'string') {
            errorMessage = error;
          }

          this.$message.error(errorMessage);
          this.loading = false;
        });
    }
  }
};
</script>

<style rel="stylesheet/scss" lang="scss">
.login {
  display: flex;
  justify-content: center;
  align-items: center;
  height: 100%;
  background-image: url("../assets/images/login-background.jpg");
  background-size: cover;
}

.title {
  margin: 0px auto 30px auto;
  text-align: center;
  color: #707070;
}

.login-form {
  border-radius: 6px;
  background: #ffffff;
  width: 400px;
  padding: 25px 25px 5px 25px;

  .el-input {
    height: 38px;

    input {
      height: 38px;
    }
  }

  .input-icon {
    height: 39px;
    width: 14px;
    margin-left: 2px;
  }
}

.login-tip {
  font-size: 13px;
  text-align: center;
  color: #bfbfbf;
}

.login-code {
  width: 33%;
  height: 38px;
  float: right;

  img {
    cursor: pointer;
    vertical-align: middle;
  }
}

.sms-login {
  width: 25%;
  height: 30px;
  float: right;


}

.el-login-footer {
  height: 40px;
  line-height: 40px;
  position: fixed;
  bottom: 0;
  width: 100%;
  text-align: center;
  color: #fff;
  font-family: Arial;
  font-size: 12px;
  letter-spacing: 1px;
}

.login-code-img {
  height: 38px;
}
</style>
