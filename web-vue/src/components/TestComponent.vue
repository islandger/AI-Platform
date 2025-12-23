<template>
  <div class="test-container">
    <h2>测试组件</h2>
    <button @click="testClick">点击我</button>
    <p v-if="clicked">按钮已点击！</p>
  </div>
</template>

<script>
export default {
  name: 'TestComponent',
  data() {
    return {
      clicked: false
    };
  },
  methods: {
    testClick() {
      console.log('按钮被点击了！');
      this.clicked = true;
    }
  }
};
</script>

<style>
.test-container {
  max-width: 400px;
  margin: 0 auto;
  padding: 2rem;
  background: white;
  border-radius: 8px;
  box-shadow: 0 2px 10px rgba(0,0,0,0.1);
}
</style>