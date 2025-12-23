// 测试API接口是否正常工作
async function testAPI() {
  console.log('测试API接口...');
  
  try {
    // 测试登录接口
    const loginRes = await fetch('http://localhost:8080/api/auth/login', {
      method: 'POST',
      headers: { 'Content-Type': 'application/json' },
      body: JSON.stringify({ username: 'test', password: 'test' })
    });
    const loginData = await loginRes.json();
    console.log('登录接口响应:', loginData);
    
    // 测试注册接口
    const registerRes = await fetch('http://localhost:8080/api/auth/register', {
      method: 'POST',
      headers: { 'Content-Type': 'application/json' },
      body: JSON.stringify({ username: 'test_new_' + Date.now(), password: 'test', display_name: 'Test User' })
    });
    const registerData = await registerRes.json();
    console.log('注册接口响应:', registerData);
    
    // 测试me接口
    if (loginData.token) {
      const meRes = await fetch('http://localhost:8080/api/auth/me', {
        method: 'GET',
        headers: { 'Authorization': 'Bearer ' + loginData.token }
      });
      const meData = await meRes.json();
      console.log('Me接口响应:', meData);
    }
    
    console.log('API测试完成');
  } catch (e) {
    console.error('API测试失败:', e);
  }
}

testAPI();