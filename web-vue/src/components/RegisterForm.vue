<template>
  <div class="form-container">
    <h2>注册</h2>
    <form @submit.prevent="handleRegister">
      <div class="form-group">
        <label>用户名</label>
        <input type="text" v-model="username" required />
      </div>
      <div class="form-group">
        <label>密码</label>
        <input type="password" v-model="password" required />
      </div>
      <div class="form-group">
        <label>显示名称</label>
        <input type="text" v-model="display_name" required />
      </div>
      <button type="submit" class="btn btn-primary full-width">注册</button>
    </form>
    <p class="form-footer">已有账号？<button @click="$emit('show-login')" class="link-btn">登录</button></p>
  </div>
</template>

<script>
import { register, saveToken, fetchCurrentUser } from '../auth.js';

export default {
  name: 'RegisterForm',
  data() { return { username: '', password: '', display_name: '' } },
  methods: {
    async handleRegister() {
      try {
        const res = await register(this.username, this.password, this.display_name);
        if (res.token) {
          saveToken(res.token);
          const user = await fetchCurrentUser();
          this.$emit('register-success', user);
        } else {
          alert('注册失败: ' + res.error || '未知错误');
        }
      } catch (e) {
        console.error('注册失败:', e);
        alert('注册失败: 服务器错误');
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