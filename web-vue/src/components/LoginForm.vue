<template>
  <div class="form-container">
    <h2>登录</h2>
    <form @submit.prevent="handleLogin">
      <div class="form-group">
        <label>用户名</label>
        <input type="text" v-model="username" required />
      </div>
      <div class="form-group">
        <label>密码</label>
        <input type="password" v-model="password" required />
      </div>
      <button type="submit" class="btn btn-primary full-width">登录</button>
    </form>
    <p class="form-footer">没有账号？<button @click="$emit('show-register')" class="link-btn">注册</button></p>
  </div>
</template>

<script>
import { login, saveToken, fetchCurrentUser } from '../auth.js';

export default {
  name: 'LoginForm',
  data() { return { username: '', password: '' } },
  methods: {
    async handleLogin() {
      try {
        const data = await login(this.username, this.password);
        if (data.token) {
          saveToken(data.token);
          const user = await fetchCurrentUser();
          this.$emit('login-success', user);
        } else {
          alert('登录失败: ' + (data.message || '未知错误'));
        }
      } catch (error) {
        console.error('登录失败:', error);
        if (error instanceof SyntaxError) {
          alert('登录失败: 用户名或密码错误。');
        } else {
          alert('登录失败: 服务器错误或网络问题。');
        }
      }
    }
  }
};
</script>

<style>
.form-container { max-width: 400px; margin: 0 auto; padding: 2rem; background: white; border-radius: 8px; box-shadow: 0 2px 10px rgba(0,0,0,0.1); }
.form-group { margin-bottom: 1rem; }
label { display: block; margin-bottom: 0.5rem; font-weight: bold; }
input { width: 100%; padding: 0.5rem; border: 1px solid #ddd; border-radius: 4px; }
.full-width { width: 100%; margin-top: 1rem; }
.form-footer { margin-top: 1rem; text-align: center; }
.link-btn { background: none; border: none; color: #3498db; cursor: pointer; text-decoration: underline; }
</style>