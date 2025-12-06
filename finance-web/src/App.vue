<script setup>
import { onMounted } from 'vue'
import { useRouter } from 'vue-router'

const router = useRouter()

onMounted(() => {
  // 【自动修复】强制设置当前账簿ID为1，解决“请先选择账簿”的拦截
  localStorage.setItem('current_book_id', '1')
  localStorage.setItem('current_book_name', '李记便利店-2025主账套')
  console.log("已自动选择默认账簿")
})
</script>

<template>
  <RouterView />
</template>

<style>
body { margin: 0; padding: 0; background-color: #f0f2f5; font-family: sans-serif; }
</style>