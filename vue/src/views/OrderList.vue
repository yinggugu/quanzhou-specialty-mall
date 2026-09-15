<template>
  <!-- 订单列表页 - 1:1复刻 order/list.jsp 结构 -->
  <div class="container">
    <div class="order-header-section">
      <h2 class="section-title">我的订单</h2>
      <p class="order-subtitle">查看和管理您的订单</p>
    </div>

    <!-- 订单列表 -->
    <div class="order-list">
      <!-- 空状态 -->
      <div v-if="!loading && orders.length === 0" class="cart-empty">
        <div class="empty-icon">📋</div>
        <p>您还没有任何订单</p>
        <router-link to="/products" class="btn btn-primary">去购物</router-link>
      </div>

      <!-- 订单卡片列表 -->
      <div v-for="order in orders" :key="order.id" class="order-item">
        <!-- 订单头部 -->
        <div class="order-item-header">
          <div class="order-header-left">
            <div class="order-number">订单号：{{ order.orderNo }}</div>
            <div class="order-time">{{ order.createTime }}</div>
          </div>
          <div class="order-header-right">
            <span class="order-status-badge" :class="getStatusClass(order.afterSaleId ? (order.afterSaleStatus === 4 ? 6 : 5) : order.status)">
              {{ order.afterSaleId ? (order.afterSaleStatus === 4 ? '售后已完结' : '售后中') : getStatusText(order.status) }}
            </span>
          </div>
        </div>

        <!-- 订单商品列表 -->
        <div class="order-item-body">
          <div
            v-for="item in order.orderItems"
            :key="item.id"
            class="order-product"
            @click="$router.push('/products/'+item.productId)"
          >
            <div class="order-product-image-wrapper">
              <img
                v-if="getProductImage(item.productImage)"
                :src="getProductImage(item.productImage)"
                :alt="item.productName"
                class="order-product-image"
              />
            </div>
            <div class="order-product-info">
              <div class="order-product-name">{{ item.productName }}</div>
              <div class="order-product-meta">
                <span class="order-product-quantity">数量：{{ item.quantity }}</span>
                <span class="order-product-price">单价：¥{{ item.productPrice }}</span>
              </div>
            </div>
          </div>
        </div>

        <!-- 订单底部 -->
        <div class="order-item-footer">
          <div class="order-footer-left">
            <span class="order-total-label">合计：</span>
            <span class="order-total-price">¥{{ order.totalPrice }}</span>
          </div>
          <div class="order-footer-right">
            <router-link :to="`/orders/${order.id}`" class="btn btn-secondary btn-sm">
              查看详情
            </router-link>
            <a
              href="javascript:void(0)"
              class="btn btn-service btn-sm"
              @click="handleService(order.id)"
            >联系客服</a>
            <a
              v-if="order.status === 0"
              href="javascript:void(0)"
              class="btn btn-primary btn-sm"
              @click="handlePay(order.id)"
            >立即付款</a>
            <a
              v-if="order.status === 0"
              href="javascript:void(0)"
              class="btn btn-danger btn-sm"
              @click="handleCancel(order.id)"
            >取消订单</a>
            <a
              v-if="order.status === 2"
              href="javascript:void(0)"
              class="btn btn-success btn-sm"
              @click="handleConfirmReceipt(order.id)"
            >确认收货</a>
            <!-- ✅ 已完成：去评价 + 申请退货/售后详情 -->
            <template v-if="order.status === 3">
              <a href="javascript:void(0)" class="btn btn-primary btn-sm" @click="$router.push('/orders/'+order.id+'/review')">去评价</a>
              <a v-if="order.afterSaleId" href="javascript:void(0)" class="btn btn-warning btn-sm" @click="$router.push('/afterSaleDetail?id='+order.afterSaleId)">售后详情</a>
              <a v-else href="javascript:void(0)" class="btn btn-danger btn-sm" @click="openReturn(order)">申请退货</a>
            </template>
          </div>
        </div>
      </div>
    </div>

    <!-- ✅ 评价弹窗 — 暖棕米杏主题 -->
    <div v-if="reviewDialogVisible" class="review-overlay" @click.self="reviewDialogVisible=false">
      <div class="review-dialog">
        <!-- 标题栏 -->
        <div class="review-dialog-header">
          <h3 class="review-dialog-title">商品评价</h3>
          <span class="review-close" @click="reviewDialogVisible=false">✕</span>
        </div>

        <!-- 星级打分 -->
        <div class="review-stars">
          <span v-for="s in 5" :key="s" class="review-star" :class="{ active: s <= reviewScore }"
                @click="reviewScore = s" @mouseenter="hoverStar = s" @mouseleave="hoverStar = 0">
            {{ s <= (hoverStar || reviewScore) ? '★' : '☆' }}
          </span>
        </div>

        <!-- 文字输入 -->
        <textarea v-model="reviewContent" class="review-textarea" rows="4"
                  placeholder="分享您的使用体验..."></textarea>

        <!-- 匿名勾选 -->
        <label class="review-anonymous">
          <span class="review-checkbox" :class="{ checked: reviewAnonymous }" @click="reviewAnonymous = !reviewAnonymous">
            <span v-if="reviewAnonymous" class="check-mark">✓</span>
          </span>
          <span class="review-anonymous-text">匿名评价</span>
        </label>

        <!-- 底部按钮 -->
        <div class="review-buttons">
          <button class="review-btn review-btn-cancel" @click="reviewDialogVisible=false">取消</button>
          <button class="review-btn review-btn-submit" @click="submitReview">提交</button>
        </div>
      </div>
    </div>

    <!-- ✅ 通用确认弹窗 -->
    <div v-if="confirmVisible" class="review-overlay"><div class="review-dialog">
      <div class="review-dialog-header"><h3 class="review-dialog-title">{{confirmTitle}}</h3><span class="review-close" @click="confirmVisible=false">✕</span></div>
      <p class="as-dialog-text">{{confirmMsg}}</p>
      <div class="review-buttons"><button class="review-btn review-btn-cancel" @click="confirmVisible=false">取消</button><button class="review-btn review-btn-submit" @click="doConfirm">{{confirmBtn}}</button></div>
    </div></div>

    <!-- ✅ 第一层：商品勾选弹窗 -->
    <div v-if="returnVisible&&returnStep===1" class="review-overlay"><div class="review-dialog" style="max-height:80vh;overflow-y:auto;">
      <div class="review-dialog-header"><h3 class="review-dialog-title">选择退货商品</h3><span class="review-close" @click="returnClose">✕</span></div>
      <div v-if="returnOrder" style="margin-bottom:16px;">
        <div v-for="item in returnOrder.orderItems" :key="item.id" class="as-check-row" @click="toggleReturnItem(item)">
          <span class="as-checkbox" :class="{checked:checkedItems.includes(item.id)}">{{checkedItems.includes(item.id)?'✓':''}}</span>
          <img v-if="getReturnImg(item.productImage)" :src="getReturnImg(item.productImage)" class="as-check-img" />
          <div class="as-check-info">
            <p class="as-check-name">{{item.productName}}</p>
            <p style="color:#999288;font-size:13px;">¥{{item.totalPrice}} × {{item.quantity}}</p>
          </div>
        </div>
      </div>
      <div class="review-buttons"><button class="review-btn review-btn-cancel" @click="returnClose">取消</button><button class="review-btn review-btn-submit" :disabled="checkedItems.length===0" @click="goReturnStep2">下一步</button></div>
    </div></div>

    <!-- ✅ 第二层：售后填写弹窗 -->
    <div v-if="returnVisible&&returnStep===2" class="review-overlay"><div class="review-dialog" style="max-height:90vh;overflow-y:auto;">
      <div class="review-dialog-header"><h3 class="review-dialog-title">申请退货</h3><span class="review-close" @click="returnClose">✕</span></div>
      <p style="color:#999288;font-size:14px;margin-bottom:4px;">退款金额（上限 ¥{{returnMaxAmount}}）</p>
      <el-input-number v-model="returnAmount" :min="0.01" :max="returnMaxAmount" :precision="2" style="width:100%;margin-bottom:12px;" />
      <select v-model="returnReason" class="as-select"><option value="">请选择退货原因</option><option value="质量瑕疵">质量瑕疵</option><option value="实物与描述不符">实物与描述不符</option><option value="不喜欢不合适">不喜欢/不合适</option><option value="漏发破损">漏发破损</option><option value="其他">其他</option></select>
      <textarea v-model="returnDesc" class="review-textarea" rows="3" :placeholder="'退货描述（必填，10-200字）'+returnDesc.length+'/200'" style="margin-top:12px;"></textarea>
      <div class="as-upload-area" style="margin-top:12px;">
        <p class="as-upload-title">上传凭证（最多3张，jpg/png，单张≤10MB）<span v-if="forceImage" style="color:#e74c3c;"> *必传</span></p>
        <div style="display:flex;gap:8px;flex-wrap:wrap;">
          <div v-for="(img,i) in returnImages" :key="i" class="as-thumb" @click="previewReturnImg=img;imgVisible=true"><img v-if="img" :src="img" /><span class="as-thumb-del" @click.stop="returnImages.splice(i,1)">✕</span></div>
          <label v-if="returnImages.length<3" class="as-upload-btn"><input type="file" accept="image/jpeg,image/png" style="display:none;" @change="onReturnImage" /><span>+</span></label>
        </div>
      </div>
      <div class="review-buttons" style="margin-top:16px;"><button class="review-btn review-btn-cancel" @click="returnStep=1">上一步</button><button class="review-btn review-btn-submit" @click="submitReturn">提交</button></div>
    </div></div>
    <el-dialog :visible.sync="imgVisible" width="500px"><img v-if="previewReturnImg" :src="previewReturnImg" style="width:100%;border-radius:8px;" /></el-dialog>

    <!-- 分页 -->
    <div v-if="totalPages > 1" class="pagination">
      <a
        v-if="currentPage > 1"
        href="javascript:void(0)"
        class="page-link"
        @click="changePage(currentPage - 1)"
      >上一页</a>
      <span class="page-info">第 {{ currentPage }} 页，共 {{ totalPages }} 页</span>
      <a
        v-if="currentPage < totalPages"
        href="javascript:void(0)"
        class="page-link"
        @click="changePage(currentPage + 1)"
      >下一页</a>
    </div>
  </div>
</template>

<script>
import { getOrderList, payOrder, cancelOrder, confirmReceipt } from '@/api/order'
import { addComment } from '@/api/product'
import request from '@/api/request'

export default {
  name: 'OrderList',

  data() {
    return {
      orders: [],
      currentPage: 1,
      totalPages: 1,
      loading: false,
      // ✅ 评价弹窗
      reviewDialogVisible: false,
      reviewOrder: null,
      reviewScore: 5,
      reviewContent: '',
      reviewAnonymous: false,
      hoverStar: 0,  // ✅ 星级hover效果
      // ✅ 售后
      returnStep: 1, returnVisible: false, returnOrder: null, returnReason: '', returnDesc: '',
      returnAmount: 0, returnMaxAmount: 0, checkedItems: [],
      returnImages: [], previewReturnImg: '', imgVisible: false,
      confirmVisible: false, confirmTitle: '', confirmMsg: '', confirmBtn: '', confirmAction: null
    }
  },

  computed: {
    isLoggedIn(){return !!this.$store.state.token},
    forceImage(){return ['质量瑕疵','实物与描述不符','漏发破损'].includes(this.returnReason)}
  },

  mounted() {
    this.loadOrders()
  },

  methods: {
    getProductImage(imagePath) {
      return this.$resolveImageUrl(imagePath)
    },

    // 订单状态文本映射 (与原JSP中c:choose逻辑一致)
    getStatusText(status) {
      const map = {
        0: '待付款',
        1: '待发货',
        2: '待收货',
        3: '已完成',
        4: '已取消',
        5: '售后处理中',
        6: '售后已完结'
      }
      return map[status] || '未知状态'
    },
    getStatusClass(status) {
      const map = {0:'s-pending',1:'s-shipping',2:'s-shipping',3:'s-done',4:'s-cancel',5:'s-after'}
      return map[status]||''
    },

    // 订单状态CSS类名
    getStatusClass(status) {
      const map = {
        0: 'status-pending',
        1: 'status-shipping',
        2: 'status-delivering',
        3: 'status-completed',
        4: 'status-cancelled'
      }
      return map[status] || 'status-unknown'
    },

    async loadOrders() {
      this.loading = true
      try {
        const res = await getOrderList({
          page: this.currentPage,
          pageSize: 10
        })
        if (res.code === 200 && res.data) {
          this.orders = res.data.list || res.data.records || []
          this.totalPages = res.data.totalPages || res.data.pages || 1
          this.currentPage = res.data.currentPage || res.data.current || 1
        }
      } catch (e) {
        console.error('加载订单列表失败:', e)
      } finally {
        this.loading = false
      }
    },

    changePage(page) {
      this.currentPage = page
      this.loadOrders()
      window.scrollTo({ top: 0, behavior: 'smooth' })
    },

    // 联系客服 - 新窗口打开
    handleService(orderId) {
      const url = this.$router.resolve({ path: '/service', query: { orderId } }).href
      window.open(url, '_blank')
    },

    // 支付订单
    handlePay(orderId) {
      this.confirmTitle = '确认支付'
      this.confirmMsg = '确认支付此订单？'
      this.confirmBtn = '确认支付'
      this.confirmAction = async () => { await payOrder(orderId); this.$message.success('支付成功'); this.loadOrders() }
      this.confirmVisible = true
    },

    // 取消订单
    handleCancel(orderId) {
      this.confirmTitle = '确认取消'
      this.confirmMsg = '确定要取消此订单吗？'
      this.confirmBtn = '确认取消'
      this.confirmAction = async () => { await cancelOrder(orderId); this.$message.success('订单已取消'); this.loadOrders() }
      this.confirmVisible = true
    },

    async doConfirm() { if (this.confirmAction) { await this.confirmAction(); this.confirmVisible = false } },

    // 确认收货
    handleConfirmReceipt(orderId) {
      this.confirmTitle = '确认收货'
      this.confirmMsg = '确认已收到商品？'
      this.confirmBtn = '确认收货'
      this.confirmAction = async () => { await confirmReceipt(orderId); this.$message.success('已确认收货'); this.loadOrders() }
      this.confirmVisible = true
    },

    // ✅ 打开评价弹窗
    openReview(order) {
      this.reviewOrder = order
      this.reviewScore = 5
      this.reviewContent = ''
      this.reviewAnonymous = false
      this.hoverStar = 0
      this.reviewDialogVisible = true
    },

    // ✅ 提交评价
    async submitReview() {
      if (!this.reviewContent.trim()) { this.$message.warning('请输入评价内容'); return }
      try {
        const item = (this.reviewOrder.orderItems && this.reviewOrder.orderItems[0]) ? this.reviewOrder.orderItems[0] : null
        const orderItemId = item ? item.id : 0
        const productId = item ? item.productId : 0
        await addComment({
          productId, orderItemId,
          score: this.reviewScore,
          content: this.reviewContent.trim(),
          imgList: '',
          isAnonymous: this.reviewAnonymous ? 1 : 0
        })
        this.$message.success('评价成功')
        this.reviewDialogVisible = false
      } catch (e) { console.error(e) }
    },

    goAfterSale(order){ this.$router.push({name:'AfterSaleDetail',query:{orderId:order.id}}) },

    // ✅ 申请退货
    openReturn(order) {
      this.returnOrder = order; this.returnStep = 1; this.returnVisible = true
      this.returnReason = ''; this.returnDesc = ''; this.returnImages = []; this.checkedItems = []
    },
    returnClose(){this.returnVisible=false;this.checkedItems=[]},
    toggleReturnItem(item){const i=this.checkedItems.indexOf(item.id);i>-1?this.checkedItems.splice(i,1):this.checkedItems.push(item.id)},
    goReturnStep2(){
      if(this.checkedItems.length===0)return
      let total=0;this.checkedItems.forEach(id=>{const it=this.returnOrder.orderItems.find(i=>i.id===id);if(it)total+=it.totalPrice})
      this.returnMaxAmount=Math.round(total*100)/100;this.returnAmount=this.returnMaxAmount;this.returnStep=2
    },
    getReturnImg(p){return this.$resolveImageUrl(p)},
    async onReturnImage(e) {
      const file = e.target.files[0]; if (!file) return
      if (!['image/jpeg','image/png'].includes(file.type)) { this.$message.warning('仅支持jpg/png格式'); return }
      if (file.size > 10*1024*1024) { this.$message.warning('图片不能超过10MB'); return }
      e.target.value = ''
      try {
        const fd = new FormData(); fd.append('file', file)
        const res = await request.post('/upload', fd, { headers: { 'Content-Type': 'multipart/form-data' } })
        if (res.code === 200 && res.data.url) { this.returnImages.push(res.data.url) }
        else { this.$message.error('上传失败') }
      } catch (err) { this.$message.error('上传失败') }
    },
    async submitReturn() {
      if (!this.returnReason) { this.$message.warning('请选择退货原因'); return }
      if (this.returnDesc.length<10||this.returnDesc.length>200){this.$message.warning('退货描述需10-200字');return}
      const forceReasons=['质量瑕疵','实物与描述不符','漏发破损']
      if(forceReasons.includes(this.returnReason)&&this.returnImages.length===0){this.$message.warning('此退货原因必须上传至少1张凭证图片');return}
      try{
        const bindIds=this.checkedItems.join(',');const prodIds=this.checkedItems.map(id=>{const it=this.returnOrder.orderItems.find(i=>i.id===id);return it?it.productId:''}).filter(Boolean).join(',')
        const res=await request.post('/afterSale/apply',{orderId:this.returnOrder.id,bindItemIds:bindIds,productIds:prodIds,returnReason:this.returnReason,descText:this.returnDesc,imgList:this.returnImages.join('||'),refundAmount:this.returnAmount})
        if(res.code===200){this.$message.success('售后申请已提交');this.returnClose();this.loadOrders()}
      }catch(e){console.error(e);if(e.message)this.$message.error(e.message)}
    }
  }
}
</script>

<style scoped>
.order-product { cursor: pointer; }
.order-product:hover { background: rgba(0,0,0,0.02); }

/* ========================================
   评价弹窗 — 暖棕米杏主题
   ======================================== */
.review-overlay {
  position: fixed; top: 0; left: 0; right: 0; bottom: 0;
  background: rgba(0,0,0,0.3); z-index: 2000;
  display: flex; align-items: center; justify-content: center;
}
.review-dialog {
  width: 460px; max-width: 92vw;
  background: #fdfaf5;  /* 浅米杏底色 */
  border-radius: 16px;  /* 与商品卡片圆角一致 */
  box-shadow: 0 2px 8px rgba(92,51,23,0.04), 0 8px 32px rgba(92,51,23,0.08), 0 20px 64px rgba(92,51,23,0.06);
  padding: 28px 30px 24px;
  box-sizing: border-box;
  font-family: inherit;
}
/* 标题栏 */
.review-dialog-header {
  display: flex; justify-content: space-between; align-items: center;
  margin-bottom: 20px;
}
.review-dialog-title {
  margin: 0; font-size: 1.1rem; font-weight: 600; color: #4a3322;
}
.review-close {
  font-size: 18px; color: #b8a899; cursor: pointer; line-height: 1;
  transition: color 0.2s;
}
.review-close:hover { color: #966c47; }

/* 星级 */
.review-stars {
  text-align: center; margin-bottom: 16px;
}
.review-star {
  font-size: 30px; cursor: pointer; color: #b8a899;  /* 未选中浅灰 */
  transition: color 0.15s, transform 0.15s;
  user-select: none;
}
.review-star.active { color: #966c47; }  /* 选中暖棕 */
.review-star:hover { color: #7d5a3a; transform: scale(1.08); }

/* 输入框 */
.review-textarea {
  width: 100%; box-sizing: border-box;
  background: #fffefc; border: 1.5px solid #d9c8b8; border-radius: 10px;
  padding: 12px 16px; font-size: 0.95rem; color: #4a3322;
  outline: none; resize: vertical; font-family: inherit;
  transition: border-color 0.2s;
}
.review-textarea:focus { border-color: #966c47; }
.review-textarea::placeholder { color: #b8a899; }

/* 匿名勾选 */
.review-anonymous {
  display: flex; align-items: center; gap: 8px;
  margin-top: 12px; cursor: pointer; user-select: none;
}
.review-checkbox {
  width: 18px; height: 18px; border: 1.5px solid #d9c8b8; border-radius: 4px;
  display: flex; align-items: center; justify-content: center;
  transition: all 0.2s; flex-shrink: 0;
}
.review-checkbox.checked {
  background: #966c47; border-color: #966c47;
}
.check-mark { color: #fff; font-size: 12px; font-weight: 600; }
.review-anonymous-text { color: #4a3322; font-size: 0.95rem; }

/* 按钮 */
.review-buttons {
  display: flex; gap: 12px; margin-top: 20px; justify-content: flex-end;
}
.review-btn {
  padding: 10px 24px; border-radius: 20px; font-size: 0.95rem;
  font-weight: 500; cursor: pointer; border: none; font-family: inherit;
  transition: all 0.2s;
}
.review-btn-cancel {
  background: #fff; color: #4a3322; border: 2px solid #966c47;
}
.review-btn-cancel:hover { background: #fdfaf5; }
.review-btn-submit {
  background: #966c47; color: #fff;
  box-shadow: 0 2px 8px rgba(150,108,71,0.3);
}
.review-btn-submit:hover { background: #7d5a3a; transform: translateY(-1px); }

/* ✅ 售后上传 */
.as-select { width:100%; padding:10px 14px; border:1.5px solid #d9c8b8; border-radius:10px; font-size:0.95rem; color:#4a3322; background:#fffefc; outline:none; margin-bottom:12px; font-family:inherit; }
.as-select:focus { border-color:#966c47; }
.as-upload-area { margin:12px 0; }
.as-upload-title { color:#b8a899; font-size:12px; margin-bottom:6px; }
.as-thumb { width:70px; height:70px; border-radius:8px; overflow:hidden; position:relative; cursor:pointer; flex-shrink:0; }
.as-thumb img { width:100%; height:100%; object-fit:cover; }
.as-thumb-del { position:absolute; top:2px; right:2px; width:18px; height:18px; background:rgba(0,0,0,0.5); color:#fff; border-radius:50%; display:flex;align-items:center;justify-content:center; font-size:10px; }
.as-upload-btn { width:70px; height:70px; border:2px dashed #d9c8b8; border-radius:8px; display:flex;align-items:center;justify-content:center; font-size:28px; color:#b8a899; cursor:pointer; transition:border-color .2s; flex-shrink:0; }
.as-upload-btn:hover { border-color:#966c47; color:#966c47; }
.as-check-row { display:flex;align-items:center;gap:12px;padding:10px 0;border-bottom:1px solid #f8f3ec;cursor:pointer; }
.as-checkbox { width:22px;height:22px;border:2px solid #d9c8b8;border-radius:4px;display:flex;align-items:center;justify-content:center;font-size:14px;color:#966c47;flex-shrink:0; }
.as-checkbox.checked { background:#966c47;border-color:#966c47;color:#fff; }
.as-check-img { width:48px;height:48px;border-radius:6px;object-fit:cover;flex-shrink:0; }
.as-check-info { flex:1;min-width:0; }
.as-check-name { color:#4a3322;font-size:15px;font-weight:500;margin:0 0 2px; }
</style>
