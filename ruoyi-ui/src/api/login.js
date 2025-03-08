import request from '@/utils/request'

// 登录方法
export function login(username, password, code, uuid) {
  const data = {
    username,
    password,
    code,
    uuid
  }
  return request({
    url: '/login',
    method: 'post',
    data: data
  })
}

// 短信登录方法
export function smsLogin(mobile, code, uuid) {
  const data = {
    mobile,
    smsCode: code,
    uuid
  }
  return request({
    url: '/system/sms/login',
    headers: {
      isToken: false
    },
    method: 'post',
    data: data
  })
}

// 简化版短信登录方法 - 避开401错误
export function simpleSmsLogin(mobile, code) {
  const data = {
    mobile,
    smsCode: code
  }
  return request({
    url: '/sms/simple-login',  // 使用新的URL路径
    headers: {
      isToken: false
    },
    method: 'post',
    data: data
  })
}

// 获取用户详细信息
export function getInfo() {
  return request({
    url: '/getInfo',
    method: 'get'
  })
}

// 退出方法
export function logout() {
  return request({
    url: '/logout',
    method: 'post'
  })
}

// 获取验证码
export function getCodeImg() {
  return request({
    url: '/captchaImage',
    method: 'get'
  })
}

// 获取短信验证码
export function getSmsCode(mobile) {
  return request({
    url: '/system/sms/code?mobile=' + mobile,
    method: 'get'
  })
}
