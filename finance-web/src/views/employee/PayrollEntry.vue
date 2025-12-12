<script setup>
/**
 * 员工薪酬发放 - 修复版
 *
 * 智能科目匹配规则：
 * 1. 工资费用科目：优先找 660201，然后找包含"工资"的EXPENSE科目，最后找任何EXPENSE科目
 * 2. 现金科目：优先找 1001/1002，然后找包含"现金"/"银行"/"货币"的ASSET科目
 */
import { ref, onMounted, computed } from 'vue'
import axios from 'axios'
import { ElMessage, ElMessageBox } from 'element-plus'

const month = ref(new Date().toISOString().slice(0, 7))
const employeeList = ref([])
const accounts = ref([])
const loading = ref(false)

// 科目匹配结果
const wageAccount = ref(null)
const cashAccount = ref(null)
const configOk = ref(false)
const configError = ref('')

// 加载数据
const loadData = async () => {
  loading.value = true
  try {
    const [resEmp, resAcc] = await Promise.all([
      axios.get('http://localhost:8080/employee/list'),
      axios.get('http://localhost:8080/financeAccount/list')
    ])

    employeeList.value = (resEmp.data || []).map(emp => ({
      ...emp,
      bonus: 0,
      deduction: 0,
      realSalary: emp.basicSalary || emp.salary || 5000
    }))
    accounts.value = resAcc.data || []

    // 自动匹配科目
    autoMatchAccounts()

  } catch (err) {
    ElMessage.error('数据加载失败，请检查后端')
  } finally {
    loading.value = false
  }
}

// 智能匹配科目
const autoMatchAccounts = () => {
  const accs = accounts.value

  // ========== 1. 查找工资费用科目 ==========
  // 优先级1: 科目代码 660201 (管理费用-工资)
  wageAccount.value = accs.find(a => a.accountCode === '660201')

  // 优先级2: 科目名称包含"工资"且类型为EXPENSE
  if (!wageAccount.value) {
    wageAccount.value = accs.find(a => {
      const name = a.accountName || ''
      const type = a.accountType || ''
      return name.includes('工资') && type === 'EXPENSE'
    })
  }

  // 优先级3: 科目代码 2203/2211 (应付职工薪酬) - 可用于计提
  if (!wageAccount.value) {
    wageAccount.value = accs.find(a => {
      const code = a.accountCode || ''
      const name = a.accountName || ''
      return code === '2203' || code === '2211' || name.includes('应付职工薪酬')
    })
  }

  // 优先级4: 任何费用类科目
  if (!wageAccount.value) {
    wageAccount.value = accs.find(a => a.accountType === 'EXPENSE')
  }

  // ========== 2. 查找现金/银行科目 ==========
  // 优先级1: 科目代码 1001 (库存现金) 或 1002 (银行存款)
  cashAccount.value = accs.find(a => {
    const code = a.accountCode || ''
    return code === '1001' || code === '1002'
  })

  // 优先级2: 科目名称包含"现金"/"银行"/"货币"
  if (!cashAccount.value) {
    cashAccount.value = accs.find(a => {
      const name = a.accountName || ''
      return name.includes('现金') || name.includes('银行') || name.includes('货币')
    })
  }

  // 优先级3: 任何资产类科目（作为兜底）
  if (!cashAccount.value) {
    cashAccount.value = accs.find(a => {
      const type = a.accountType || ''
      const code = a.accountCode || ''
      return type === 'ASSET' && code.startsWith('1')
    })
  }

  // ========== 3. 检查配置结果 ==========
  checkConfig()
}

// 检查配置是否完整
const checkConfig = () => {
  const errors = []

  if (!wageAccount.value) {
    errors.push('找不到工资费用科目（建议添加: 660201 管理费用-工资）')
  }

  if (!cashAccount.value) {
    errors.push('找不到现金/银行科目（建议添加: 1001 库存现金 或 1002 银行存款）')
  }

  if (errors.length > 0) {
    configOk.value = false
    configError.value = errors.join('；')
  } else {
    configOk.value = true
    configError.value = ''
  }
}

// 计算实发工资
const calculateTotal = (row) => {
  row.realSalary = Number(row.basicSalary || 0) + Number(row.bonus || 0) - Number(row.deduction || 0)
  if (row.realSalary < 0) row.realSalary = 0
}

// 工资总额
const totalPayroll = computed(() => {
  return employeeList.value.reduce((sum, item) => sum + Number(item.realSalary || 0), 0)
})

// 发放工资
const submitPayroll = async () => {
  if (employeeList.value.length === 0) {
    return ElMessage.warning('没有员工数据')
  }

  if (!configOk.value) {
    return ElMessage.error('请先配置正确的会计科目，或执行 complete_accounts.sql 补充科目')
  }

  if (totalPayroll.value <= 0) {
    return ElMessage.warning('工资总额为0，无需发放')
  }

  ElMessageBox.confirm(
      `确认发放 ${month.value} 工资，共 ¥${totalPayroll.value.toLocaleString()}？`,
      '确认发放',
      { type: 'warning' }
  ).then(async () => {
    try {
      const transaction = {
        description: `${month.value} 工资发放`,
        voucherDate: new Date().toISOString().split('T')[0],
        status: 'POSTED',
        splits: [
          {
            accountId: wageAccount.value.accountId,
            summary: `${month.value} 员工工资`,
            dcDirection: 1,  // 借方
            amount: totalPayroll.value
          },
          {
            accountId: cashAccount.value.accountId,
            summary: `${month.value} 工资发放`,
            dcDirection: -1,  // 贷方
            amount: totalPayroll.value
          }
        ]
      }

      const res = await axios.post('http://localhost:8080/financeTransaction/add', transaction)

      if (res.data.code === 200) {
        ElMessage.success(`工资发放成功！凭证号: ${res.data.transactionId}`)
      } else {
        ElMessage.error(res.data.message || '发放失败')
      }
    } catch (e) {
      ElMessage.error('发放失败: ' + (e.message || '网络错误'))
    }
  }).catch(() => {})
}

onMounted(() => loadData())
</script>

<template>
  <div class="page-container">
    <div class="header">
      <h3>💰 员工薪酬发放</h3>
      <div class="actions">
        <span>月份：</span>
        <el-date-picker
            v-model="month"
            type="month"
            value-format="YYYY-MM"
            :clearable="false"
            style="width: 140px; margin-right: 10px;"
        />
        <el-button
            type="primary"
            @click="submitPayroll"
            :disabled="!configOk || totalPayroll <= 0"
        >
          一键发放 (¥{{ totalPayroll.toLocaleString() }})
        </el-button>
      </div>
    </div>

    <!-- 科目配置提示 -->
    <el-alert
        v-if="!configOk"
        :title="'⚠️ ' + configError"
        type="error"
        show-icon
        :closable="false"
        style="margin-bottom: 15px;"
    >
      <template #default>
        <p style="margin: 5px 0;">请执行 <code>complete_accounts.sql</code> 脚本补充会计科目，或手动添加以下科目：</p>
        <ul style="margin: 5px 0 0 20px; font-size: 13px;">
          <li>1001 库存现金 或 1002 银行存款（资产类）</li>
          <li>660201 管理费用-工资（费用类）</li>
        </ul>
      </template>
    </el-alert>

    <el-alert
        v-else
        type="success"
        show-icon
        :closable="false"
        style="margin-bottom: 15px;"
    >
      <template #title>✅ 科目配置正确</template>
      <template #default>
        <span style="font-size: 13px;">
          工资科目: {{ wageAccount?.accountName }} ({{ wageAccount?.accountCode }}) |
          付款科目: {{ cashAccount?.accountName }} ({{ cashAccount?.accountCode }})
        </span>
      </template>
    </el-alert>

    <!-- 员工列表 -->
    <el-table :data="employeeList" border stripe v-loading="loading">
      <el-table-column prop="name" label="姓名" width="120" />
      <el-table-column prop="basicSalary" label="基本工资" width="120">
        <template #default="s">
          <span>¥ {{ (s.row.basicSalary || 0).toLocaleString() }}</span>
        </template>
      </el-table-column>
      <el-table-column label="奖金 (+)" width="130">
        <template #default="s">
          <el-input-number
              v-model="s.row.bonus"
              :min="0"
              :controls="false"
              size="small"
              style="width: 100%"
              @change="calculateTotal(s.row)"
          />
        </template>
      </el-table-column>
      <el-table-column label="扣款 (-)" width="130">
        <template #default="s">
          <el-input-number
              v-model="s.row.deduction"
              :min="0"
              :controls="false"
              size="small"
              style="width: 100%"
              @change="calculateTotal(s.row)"
          />
        </template>
      </el-table-column>
      <el-table-column label="实发" width="120">
        <template #default="s">
          <strong style="color: #67c23a;">¥ {{ s.row.realSalary.toLocaleString() }}</strong>
        </template>
      </el-table-column>

      <template #empty>
        <div style="padding: 30px; text-align: center; color: #999;">
          暂无员工数据，请先在「员工花名册」中添加员工
        </div>
      </template>
    </el-table>

    <!-- 合计 -->
    <div class="summary-bar" v-if="employeeList.length > 0">
      <span>共 {{ employeeList.length }} 人</span>
      <span>工资总额: <b style="color: #f56c6c; font-size: 18px;">¥ {{ totalPayroll.toLocaleString() }}</b></span>
    </div>

    <!-- 会计分录预览 -->
    <el-card v-if="configOk && totalPayroll > 0" shadow="never" style="margin-top: 20px;">
      <template #header>📝 会计分录预览</template>
      <div class="entry-preview">
        <div class="entry-row">
          <span class="direction debit">借</span>
          <span class="account">{{ wageAccount?.accountName }}</span>
          <span class="amount">¥ {{ totalPayroll.toLocaleString() }}</span>
        </div>
        <div class="entry-row">
          <span class="direction credit">贷</span>
          <span class="account">{{ cashAccount?.accountName }}</span>
          <span class="amount">¥ {{ totalPayroll.toLocaleString() }}</span>
        </div>
      </div>
    </el-card>
  </div>
</template>

<style scoped>
.page-container {
  background: #fff;
  padding: 20px;
  border-radius: 8px;
}

.header {
  display: flex;
  justify-content: space-between;
  align-items: center;
  margin-bottom: 20px;
  padding-bottom: 15px;
  border-bottom: 1px solid #eee;
}

.header h3 {
  margin: 0;
}

.actions {
  display: flex;
  align-items: center;
}

.summary-bar {
  display: flex;
  justify-content: space-between;
  padding: 15px;
  background: #f5f7fa;
  margin-top: 15px;
  border-radius: 4px;
}

.entry-preview {
  padding: 10px;
  background: #fafafa;
  border-radius: 4px;
}

.entry-row {
  display: flex;
  align-items: center;
  padding: 10px;
  margin-bottom: 5px;
  background: white;
  border-radius: 4px;
}

.entry-row:last-child {
  margin-bottom: 0;
}

.direction {
  width: 30px;
  text-align: center;
  padding: 3px 8px;
  border-radius: 4px;
  font-weight: bold;
  font-size: 12px;
  margin-right: 15px;
}

.direction.debit {
  background: #e1f3d8;
  color: #67c23a;
}

.direction.credit {
  background: #fde2e2;
  color: #f56c6c;
}

.account {
  flex: 1;
  font-weight: 500;
}

.amount {
  font-family: 'Courier New', monospace;
  font-weight: bold;
}

code {
  background: #f5f5f5;
  padding: 2px 6px;
  border-radius: 3px;
  font-size: 12px;
}
</style>