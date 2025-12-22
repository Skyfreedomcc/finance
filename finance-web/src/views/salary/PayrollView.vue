<script setup>
/**
 * 员工工资发放
 *
 * 根据科目表匹配：
 * - 工资科目：应付职工薪酬(2203/2211) 或 管理费用-工资(660201)
 * - 银行科目：货币资金(1001)
 *
 * 会计分录：
 * 方案1（计提工资）：借：管理费用-工资，贷：应付职工薪酬
 * 方案2（发放工资）：借：应付职工薪酬，贷：货币资金
 * 简化方案（直接发放）：借：管理费用-工资，贷：货币资金
 */
import { ref, onMounted, computed } from 'vue'
import axios from 'axios'
import { ElMessage } from 'element-plus'

const employees = ref([])
const accounts = ref([])
const loading = ref(false)

// 当前月份
const currentMonth = ref(new Date().toISOString().slice(0, 7))

// 科目匹配结果
const expenseAccount = ref(null)   // 费用科目：管理费用-工资
const payableAccount = ref(null)   // 负债科目：应付职工薪酬
const cashAccount = ref(null)      // 现金科目：货币资金/银行存款

// 发放方式
const payMethod = ref('direct')  // direct=直接发放, accrual=先计提后发放

// 配置状态
const configOk = ref(false)
const configError = ref('')

// 加载数据
const loadData = async () => {
  loading.value = true
  try {
    // 加载员工列表
    const empRes = await axios.get('/employee/list')
    employees.value = (empRes.data || []).map(emp => ({
      ...emp,
      baseSalary: emp.baseSalary || emp.salary || 5000,
      bonus: 0,
      deduction: 0,
      total: emp.baseSalary || emp.salary || 5000
    }))

    // 加载科目列表
    const accRes = await axios.get('/financeAccount/list')
    accounts.value = accRes.data || []

    // 自动匹配科目
    autoMatchAccounts()

  } catch (e) {
    ElMessage.error('数据加载失败')
  } finally {
    loading.value = false
  }
}

// 自动匹配科目
const autoMatchAccounts = () => {
  // 1. 查找费用科目：管理费用-工资 (660201) 或包含"工资"的费用科目
  expenseAccount.value = accounts.value.find(a => {
    const code = a.accountCode || ''
    const name = a.accountName || ''
    const type = a.accountType || ''
    return code === '660201' ||
      (name.includes('工资') && (type === 'EXPENSE' || name.includes('费用')))
  })

  // 2. 查找负债科目：应付职工薪酬 (2203/2211)
  payableAccount.value = accounts.value.find(a => {
    const code = a.accountCode || ''
    const name = a.accountName || ''
    return code === '2203' || code === '2211' || name.includes('应付职工薪酬')
  })

  // 3. 查找现金科目：货币资金(1001)、库存现金、银行存款
  cashAccount.value = accounts.value.find(a => {
    const code = a.accountCode || ''
    const name = a.accountName || ''
    return code === '1001' || code === '1002' ||
      name.includes('货币资金') ||
      name.includes('银行存款') ||
      name.includes('库存现金') ||
      name.includes('现金')
  })

  // 检查配置
  checkConfig()
}

// 检查配置是否完整
const checkConfig = () => {
  const errors = []

  if (!cashAccount.value) {
    errors.push('找不到现金/银行科目（如：货币资金、银行存款）')
  }

  if (payMethod.value === 'direct') {
    if (!expenseAccount.value) {
      errors.push('找不到工资费用科目（如：管理费用-工资）')
    }
  } else {
    if (!expenseAccount.value) {
      errors.push('找不到工资费用科目（如：管理费用-工资）')
    }
    if (!payableAccount.value) {
      errors.push('找不到应付职工薪酬科目')
    }
  }

  if (errors.length > 0) {
    configOk.value = false
    configError.value = errors.join('；')
  } else {
    configOk.value = true
    configError.value = ''
  }
}

// 计算员工实发工资
const calcTotal = (emp) => {
  emp.total = Number(emp.baseSalary || 0) + Number(emp.bonus || 0) - Number(emp.deduction || 0)
  if (emp.total < 0) emp.total = 0
}

// 总工资
const grandTotal = computed(() => {
  return employees.value.reduce((sum, emp) => sum + Number(emp.total || 0), 0)
})

// 会计分录预览
const splitsPreview = computed(() => {
  const splits = []
  if (grandTotal.value <= 0) return splits

  if (payMethod.value === 'direct') {
    // 直接发放：借-管理费用-工资，贷-货币资金
    splits.push({
      direction: '借',
      dirClass: 'debit',
      accountName: expenseAccount.value?.accountName || '管理费用-工资',
      amount: grandTotal.value
    })
    splits.push({
      direction: '贷',
      dirClass: 'credit',
      accountName: cashAccount.value?.accountName || '货币资金',
      amount: grandTotal.value
    })
  } else {
    // 计提+发放
    // 步骤1：计提 借-管理费用-工资，贷-应付职工薪酬
    // 步骤2：发放 借-应付职工薪酬，贷-货币资金
    splits.push({
      direction: '借',
      dirClass: 'debit',
      accountName: expenseAccount.value?.accountName || '管理费用-工资',
      amount: grandTotal.value,
      step: '计提'
    })
    splits.push({
      direction: '贷',
      dirClass: 'credit',
      accountName: payableAccount.value?.accountName || '应付职工薪酬',
      amount: grandTotal.value,
      step: '计提'
    })
    splits.push({
      direction: '借',
      dirClass: 'debit',
      accountName: payableAccount.value?.accountName || '应付职工薪酬',
      amount: grandTotal.value,
      step: '发放'
    })
    splits.push({
      direction: '贷',
      dirClass: 'credit',
      accountName: cashAccount.value?.accountName || '货币资金',
      amount: grandTotal.value,
      step: '发放'
    })
  }

  return splits
})

// 监听发放方式变化
const onPayMethodChange = () => {
  checkConfig()
}

// 发放工资
const submitPayroll = async () => {
  if (!configOk.value) {
    ElMessage.error('请先配置正确的会计科目')
    return
  }

  if (grandTotal.value <= 0) {
    ElMessage.warning('工资总额为0，无需发放')
    return
  }

  loading.value = true
  try {
    const description = `${currentMonth.value} 员工工资发放`

    if (payMethod.value === 'direct') {
      // 直接发放：一张凭证
      const splits = [
        {
          accountId: expenseAccount.value.accountId,
          summary: description,
          dcDirection: 1,  // 借
          amount: grandTotal.value
        },
        {
          accountId: cashAccount.value.accountId,
          summary: description,
          dcDirection: -1,  // 贷
          amount: grandTotal.value
        }
      ]

      const res = await axios.post('/financeTransaction/add', {
        voucherDate: new Date().toISOString().split('T')[0],
        description: description,
        status: 'POSTED',
        splits: splits
      })

      if (res.data.code === 200) {
        ElMessage.success(`工资发放成功！凭证号: ${res.data.transactionId}`)
      } else {
        ElMessage.error(res.data.message || '发放失败')
      }
    } else {
      // 计提+发放：两张凭证
      // 凭证1：计提
      const accrualSplits = [
        {
          accountId: expenseAccount.value.accountId,
          summary: `${currentMonth.value} 计提工资`,
          dcDirection: 1,
          amount: grandTotal.value
        },
        {
          accountId: payableAccount.value.accountId,
          summary: `${currentMonth.value} 计提工资`,
          dcDirection: -1,
          amount: grandTotal.value
        }
      ]

      const res1 = await axios.post('/financeTransaction/add', {
        voucherDate: new Date().toISOString().split('T')[0],
        description: `${currentMonth.value} 计提工资`,
        status: 'POSTED',
        splits: accrualSplits
      })

      // 凭证2：发放
      const paySplits = [
        {
          accountId: payableAccount.value.accountId,
          summary: `${currentMonth.value} 发放工资`,
          dcDirection: 1,
          amount: grandTotal.value
        },
        {
          accountId: cashAccount.value.accountId,
          summary: `${currentMonth.value} 发放工资`,
          dcDirection: -1,
          amount: grandTotal.value
        }
      ]

      const res2 = await axios.post('/financeTransaction/add', {
        voucherDate: new Date().toISOString().split('T')[0],
        description: `${currentMonth.value} 发放工资`,
        status: 'POSTED',
        splits: paySplits
      })

      if (res1.data.code === 200 && res2.data.code === 200) {
        ElMessage.success(`工资计提并发放成功！凭证号: ${res1.data.transactionId}, ${res2.data.transactionId}`)
      } else {
        ElMessage.error('部分凭证保存失败')
      }
    }

  } catch (e) {
    ElMessage.error('发放失败: ' + (e.message || '网络错误'))
  } finally {
    loading.value = false
  }
}

onMounted(() => loadData())
</script>

<template>
  <div class="payroll-page" v-loading="loading">
    <div class="page-header">
      <h2>💰 员工薪酬发放</h2>
      <div class="header-right">
        <span>月份：</span>
        <el-date-picker
          v-model="currentMonth"
          type="month"
          value-format="YYYY-MM"
          style="width: 150px"
        />
        <el-button type="primary" @click="submitPayroll" :disabled="!configOk || grandTotal <= 0">
          确认发放 (¥{{ grandTotal.toLocaleString() }})
        </el-button>
      </div>
    </div>

    <!-- 科目配置提示 -->
    <el-alert
      v-if="!configOk"
      :title="configError"
      type="error"
      show-icon
      :closable="false"
      style="margin-bottom: 20px;"
    >
      <template #default>
        <p>请检查【基础设置-会计科目表】是否包含以下科目：</p>
        <ul style="margin: 5px 0 0 20px;">
          <li>货币资金/银行存款/库存现金（资产类）</li>
          <li>管理费用-工资 或 应付职工薪酬（费用/负债类）</li>
        </ul>
      </template>
    </el-alert>

    <el-alert
      v-else
      type="success"
      show-icon
      :closable="false"
      style="margin-bottom: 20px;"
    >
      <template #title>
        ✅ 科目配置正确
      </template>
      <template #default>
        费用科目：{{ expenseAccount?.accountName }} ({{ expenseAccount?.accountCode }}) |
        现金科目：{{ cashAccount?.accountName }} ({{ cashAccount?.accountCode }})
      </template>
    </el-alert>

    <el-row :gutter="20">
      <!-- 左侧：员工列表 -->
      <el-col :span="14">
        <el-card shadow="never">
          <template #header>
            <div style="display: flex; justify-content: space-between; align-items: center;">
              <span>👥 员工工资明细</span>
              <el-radio-group v-model="payMethod" size="small" @change="onPayMethodChange">
                <el-radio-button value="direct">直接发放</el-radio-button>
                <el-radio-button value="accrual">计提后发放</el-radio-button>
              </el-radio-group>
            </div>
          </template>

          <el-table :data="employees" border stripe size="small">
            <el-table-column prop="name" label="姓名" width="100" />
            <el-table-column label="基本工资" width="130">
              <template #default="scope">
                <el-input-number
                  v-model="scope.row.baseSalary"
                  :min="0"
                  :precision="2"
                  :controls="false"
                  size="small"
                  style="width: 100%"
                  @change="calcTotal(scope.row)"
                />
              </template>
            </el-table-column>
            <el-table-column label="奖金(+)" width="100">
              <template #default="scope">
                <el-input-number
                  v-model="scope.row.bonus"
                  :min="0"
                  :precision="2"
                  :controls="false"
                  size="small"
                  style="width: 100%"
                  @change="calcTotal(scope.row)"
                />
              </template>
            </el-table-column>
            <el-table-column label="扣款(-)" width="100">
              <template #default="scope">
                <el-input-number
                  v-model="scope.row.deduction"
                  :min="0"
                  :precision="2"
                  :controls="false"
                  size="small"
                  style="width: 100%"
                  @change="calcTotal(scope.row)"
                />
              </template>
            </el-table-column>
            <el-table-column label="实发" width="120">
              <template #default="scope">
                <span class="total-amount">¥ {{ scope.row.total.toLocaleString() }}</span>
              </template>
            </el-table-column>
          </el-table>

          <div class="summary-bar">
            <span>员工人数: {{ employees.length }} 人</span>
            <span>工资总额: <b>¥ {{ grandTotal.toLocaleString() }}</b></span>
          </div>
        </el-card>
      </el-col>

      <!-- 右侧：会计分录预览 -->
      <el-col :span="10">
        <el-card shadow="never" class="preview-card">
          <template #header>📝 会计分录预览</template>

          <div v-if="splitsPreview.length > 0" class="splits-list">
            <template v-if="payMethod === 'accrual'">
              <div class="step-title">第一步：计提工资</div>
            </template>

            <div
              v-for="(split, idx) in splitsPreview"
              :key="idx"
              class="split-row"
              :class="{ 'new-step': split.step === '发放' && idx > 0 && splitsPreview[idx-1].step !== '发放' }"
            >
              <template v-if="split.step === '发放' && idx > 0 && splitsPreview[idx-1].step !== '发放'">
                <div class="step-title" style="margin-top: 15px;">第二步：发放工资</div>
              </template>
              <div class="split-content">
                <span class="direction" :class="split.dirClass">{{ split.direction }}</span>
                <span class="account">{{ split.accountName }}</span>
                <span class="amount">¥ {{ split.amount.toLocaleString() }}</span>
              </div>
            </div>
          </div>

          <div v-else class="empty-tip">
            请在左侧填写工资数据
          </div>

          <!-- 说明 -->
          <div class="explain-box">
            <p><b>📖 会计处理说明：</b></p>
            <div v-if="payMethod === 'direct'">
              <p>直接发放模式：</p>
              <p>• 借：管理费用-工资（费用增加）</p>
              <p>• 贷：货币资金（资产减少）</p>
            </div>
            <div v-else>
              <p>计提后发放模式：</p>
              <p>① 计提：借-管理费用-工资，贷-应付职工薪酬</p>
              <p>② 发放：借-应付职工薪酬，贷-货币资金</p>
            </div>
          </div>
        </el-card>
      </el-col>
    </el-row>
  </div>
</template>

<style scoped>
.payroll-page {
  padding: 20px;
}

.page-header {
  display: flex;
  justify-content: space-between;
  align-items: center;
  margin-bottom: 20px;
}

.page-header h2 {
  margin: 0;
}

.header-right {
  display: flex;
  align-items: center;
  gap: 15px;
}

.total-amount {
  color: #f56c6c;
  font-weight: bold;
}

.summary-bar {
  display: flex;
  justify-content: space-between;
  padding: 15px;
  background: #f5f7fa;
  margin-top: 15px;
  border-radius: 4px;
}

.summary-bar b {
  color: #f56c6c;
  font-size: 18px;
}

.preview-card {
  background: #fafafa;
}

.splits-list {
  background: white;
  border: 1px solid #eee;
  border-radius: 4px;
  padding: 10px;
}

.step-title {
  font-weight: bold;
  color: #409eff;
  margin-bottom: 10px;
  padding-bottom: 5px;
  border-bottom: 1px dashed #eee;
}

.split-row {
  margin-bottom: 8px;
}

.split-content {
  display: flex;
  align-items: center;
  padding: 8px;
  background: #fafafa;
  border-radius: 4px;
}

.direction {
  width: 30px;
  text-align: center;
  padding: 2px 6px;
  border-radius: 4px;
  font-weight: bold;
  margin-right: 10px;
  font-size: 12px;
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
}

.amount {
  font-weight: bold;
  font-family: monospace;
}

.empty-tip {
  text-align: center;
  padding: 40px;
  color: #999;
}

.explain-box {
  margin-top: 20px;
  padding: 15px;
  background: #ecf5ff;
  border-radius: 4px;
  font-size: 13px;
  color: #409eff;
}

.explain-box p {
  margin: 5px 0;
}
</style>