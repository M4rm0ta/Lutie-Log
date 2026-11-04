package com.example.lutielog.data

import com.example.lutielog.data.entity.Exercise

/**
 * 内置常用动作库（单一数据源）：新装预填、老版本升级时幂等补全/回填都用它。
 * 分类参考开源动作库 free-exercise-db 的身体部位体系，结合健身房高频动作精简整理。
 *
 * 记录模型：
 * - trackMode：「sets」按组（力量/支撑，多组+组间歇）；「single」单条（有氧，不问组数）
 * - metrics：有序录入字段，见 MetricSpec。例：
 *     负重力量 = "weight,reps"；自重 = "reps"；计时支撑 = "dur_sec"
 *     跑步机 = "dur_min,speed,incline,dist_km,calories"
 * - type：「compound」复合 / 「isolate」孤立
 */
object DefaultExercises {

    fun all(): List<Exercise> = buildList {
        var order = 0
        fun add(
            name: String,
            group: String,
            type: String = "isolate",
            metrics: String = "weight,reps",
            trackMode: String = "sets"
        ) {
            order += 1
            val recordType = when {
                trackMode == "single" -> "time"
                metrics.contains("weight") || metrics.contains("reps") -> "weight"
                else -> "time"
            }
            add(
                Exercise(
                    name = name,
                    muscleGroup = group,
                    type = type,
                    unit = "kg",
                    recordType = recordType,
                    trackMode = trackMode,
                    metrics = metrics,
                    isCustom = false,
                    isFavorite = false,
                    sortOrder = order
                )
            )
        }

        // —— 胸 ——（均为按组力量；俯卧撑/双杠臂屈伸为自重只记次数）
        add("杠铃卧推", "胸", "compound")
        add("上斜杠铃卧推", "胸", "compound")
        add("哑铃卧推", "胸", "compound")
        add("上斜哑铃卧推", "胸", "compound")
        add("下斜哑铃卧推", "胸", "compound")
        add("坐姿器械推胸", "胸", "compound")
        add("双杠臂屈伸", "胸", "compound", metrics = "reps")
        add("俯卧撑", "胸", "compound", metrics = "reps")
        add("平板哑铃飞鸟", "胸")
        add("上斜哑铃飞鸟", "胸")
        add("蝴蝶机夹胸", "胸")
        add("绳索夹胸", "胸")
        add("下斜杠铃卧推", "胸", "compound")
        add("仰卧哑铃上提", "胸", "compound")
        add("史密斯机卧推", "胸", "compound")
        add("上斜绳索飞鸟", "胸")
        add("单臂绳索夹胸", "胸")
        add("负重俯卧撑", "胸", "compound")
        add("宽距俯卧撑", "胸", "compound", metrics = "reps")
        add("下斜俯卧撑", "胸", "compound", metrics = "reps")
        add("跪姿俯卧撑", "胸", "compound", metrics = "reps")
        add("弹力带卧推", "胸", "compound", metrics = "reps")

        // —— 背 ——（高位下拉覆盖不同握把/握距；引体向上、反向划船、山羊挺身自重）
        add("引体向上", "背", "compound", metrics = "reps")
        add("负重引体向上", "背", "compound")
        add("高位下拉", "背", "compound")            // 宽握正握长杆
        add("反握高位下拉", "背", "compound")        // 反握长杆
        add("V把高位下拉", "背", "compound")         // 对握中立握 V 把
        add("窄握高位下拉", "背", "compound")        // 窄距正握短杆
        add("颈后高位下拉", "背", "compound")
        add("单臂高位下拉", "背", "compound")
        add("杠铃划船", "背", "compound")
        add("反握杠铃划船", "背", "compound")
        add("哑铃划船", "背", "compound")
        add("单臂哑铃划船", "背", "compound")
        add("俯卧上斜哑铃划船", "背", "compound")
        add("T杠划船", "背", "compound")
        add("坐姿划船", "背", "compound")
        add("反向划船", "背", "compound", metrics = "reps")
        add("直臂下压", "背")
        add("山羊挺身", "背", "compound", metrics = "reps")
        add("中立握引体向上", "背", "compound", metrics = "reps")
        add("助力引体向上", "背", "compound")
        add("潘德雷划船", "背", "compound")
        add("梅多斯划船", "背", "compound")
        add("单臂绳索划船", "背", "compound")
        add("器械划船", "背", "compound")
        add("架上拉", "背", "compound")
        add("弹力带划船", "背", "compound", metrics = "reps")
        add("肩胛引体向上", "背", "compound", metrics = "reps")
        add("毛巾引体向上", "背", "compound", metrics = "reps")
        add("弹力带高位下拉", "背", "compound", metrics = "reps")

        // —— 腿 ——
        add("杠铃深蹲", "腿", "compound")
        add("前蹲", "腿", "compound")
        add("哈克深蹲", "腿", "compound")
        add("杯式深蹲", "腿", "compound")
        add("腿举", "腿", "compound")
        add("箭步蹲", "腿", "compound")
        add("保加利亚分腿蹲", "腿", "compound")
        add("硬拉", "腿", "compound")
        add("罗马尼亚硬拉", "腿", "compound")
        add("相扑硬拉", "腿", "compound")
        add("早安式", "腿", "compound")
        add("臀推", "腿", "compound")
        add("腿屈伸", "腿")
        add("腿弯举", "腿")
        add("器械髋外展", "腿")
        add("器械髋内收", "腿")
        add("提踵", "腿")
        add("坐姿提踵", "腿")
        add("腿举提踵", "腿")
        add("坐姿腿弯举", "腿")
        add("哑铃箭步走", "腿", "compound")
        add("绳索后踢腿", "腿")
        add("史密斯机深蹲", "腿", "compound")
        add("腰带深蹲", "腿", "compound")
        add("六角杠铃硬拉", "腿", "compound")
        add("单腿罗马尼亚硬拉", "腿", "compound")
        add("反向箭步蹲", "腿", "compound")
        add("分腿蹲", "腿", "compound")
        add("踏板上步", "腿", "compound")
        add("俯卧腿弯举", "腿", "compound")
        add("北欧挺", "腿", "compound", metrics = "reps")
        add("哥萨克深蹲", "腿", "compound", metrics = "reps")
        add("徒手深蹲", "腿", "compound", metrics = "reps")
        add("靠墙静蹲", "腿", "compound", metrics = "dur_sec")
        add("深蹲跳", "腿", "compound", metrics = "reps")
        add("地雷管深蹲", "腿", "compound")
        add("单腿提踵", "腿", "compound", metrics = "reps")
        add("杠铃臀桥", "腿", "compound")
        add("哑铃臀推", "腿", "compound")
        add("单腿臀桥", "腿", "compound", metrics = "reps")
        add("绳索拉穿", "腿", "compound")
        add("反向挺身", "腿", "compound")
        add("驴踢", "腿", metrics = "reps")
        add("蚌式开合", "腿", metrics = "reps")
        add("侧躺髋外展", "腿", metrics = "reps")
        add("青蛙泵", "腿", metrics = "reps")
        add("消防栓式", "腿", metrics = "reps")
        add("器械臀后踢", "腿", "compound")
        add("弹力带臀桥", "腿", "compound", metrics = "reps")
        add("弹力带侧向行走", "腿", "compound", metrics = "reps")
        add("弹力带怪兽行走", "腿", "compound", metrics = "reps")
        add("弹力带深蹲", "腿", "compound", metrics = "reps")

        // —— 肩 ——
        add("杠铃推举", "肩", "compound")
        add("哑铃推举", "肩", "compound")
        add("坐姿器械推肩", "肩", "compound")
        add("阿诺德推举", "肩")
        add("哑铃侧平举", "肩")
        add("器械侧平举", "肩")
        add("哑铃前平举", "肩")
        add("直立划船", "肩", "compound")
        add("反向飞鸟", "肩")
        add("面拉", "肩")
        add("杠铃耸肩", "肩")
        add("坐姿杠铃推举", "肩", "compound")
        add("绳索侧平举", "肩")
        add("绳索反向飞鸟", "肩")
        add("哑铃耸肩", "肩")
        add("站姿哑铃推举", "肩", "compound")
        add("借力推举", "肩", "compound")
        add("地雷管推举", "肩", "compound")
        add("杠铃片前平举", "肩")
        add("绳索前平举", "肩")
        add("反向蝴蝶机", "肩")
        add("倒立撑", "肩", "compound", metrics = "reps")
        add("弹力带面拉", "肩", "compound", metrics = "reps")

        // —— 手臂 ——
        add("杠铃弯举", "手臂")
        add("哑铃弯举", "手臂")
        add("交替哑铃弯举", "手臂")
        add("锤式弯举", "手臂")
        add("传教士弯举", "手臂")
        add("集中弯举", "手臂")
        add("绳索弯举", "手臂")
        add("窄距卧推", "手臂", "compound")
        add("绳索下压", "手臂")
        add("仰卧臂屈伸", "手臂")
        add("颈后哑铃臂屈伸", "手臂")
        add("杠铃腕弯举", "手臂")
        add("EZ杠弯举", "手臂")
        add("上斜哑铃弯举", "手臂")
        add("反握杠铃弯举", "手臂")
        add("俯身哑铃臂屈伸", "手臂")
        add("器械三头臂屈伸", "手臂")
        add("蜘蛛弯举", "手臂")
        add("拖拽弯举", "手臂")
        add("绳索锤式弯举", "手臂")
        add("板凳臂屈伸", "手臂", "compound", metrics = "reps")
        add("单臂哑铃臂屈伸", "手臂")
        add("双哑铃仰卧臂屈伸", "手臂")
        add("腕屈伸", "手臂")
        add("反向弯举", "手臂")
        add("颈后绳索臂屈伸", "手臂")

        // —— 核心 ——（平板/侧平板/登山跑按组计时；其余自重动作只记次数）
        add("平板支撑", "核心", metrics = "dur_sec")
        add("侧平板支撑", "核心", metrics = "dur_sec")
        add("卷腹", "核心", metrics = "reps")
        add("仰卧起坐", "核心", metrics = "reps")
        add("反向卷腹", "核心", metrics = "reps")
        add("悬垂举腿", "核心", metrics = "reps")
        add("自行车卷腹", "核心", metrics = "reps")
        add("俄罗斯转体", "核心", metrics = "reps")
        add("死虫式", "核心", metrics = "reps")
        add("登山跑", "核心", metrics = "dur_sec")
        add("健腹轮", "核心", metrics = "reps")
        add("绳索卷腹", "核心")
        add("哑铃侧屈", "核心")
        add("绳索伐木", "核心")
        add("仰卧举腿", "核心", metrics = "reps")
        add("鸟狗式", "核心", metrics = "reps")
        add("帕洛夫推举", "核心", "compound")
        add("半跪帕洛夫推举", "核心", "compound")
        add("悬垂提膝", "核心", metrics = "reps")
        add("罗马椅提膝", "核心", "compound")
        add("下斜仰卧起坐", "核心", metrics = "reps")
        add("负重卷腹", "核心", "compound")
        add("V字两头起", "核心", metrics = "reps")
        add("仰卧交替抬腿", "核心", metrics = "reps")
        add("空心体保持", "核心", metrics = "dur_sec")
        add("龙旗", "核心", "compound", metrics = "reps")
        add("平板交替触肩", "核心", metrics = "reps")
        add("熊爬", "核心", "compound", metrics = "reps")
        add("弹力带帕洛夫", "核心", "compound", metrics = "reps")
        add("弹力带伐木", "核心", "compound", metrics = "reps")

        // —— 有氧 / 体能 ——（单条记录、多指标，不按组）
        add("跑步机", "有氧", metrics = "dur_min,speed,incline,dist_km,calories", trackMode = "single")
        add("动感单车", "有氧", metrics = "dur_min,resistance,dist_km,calories", trackMode = "single")
        add("椭圆机", "有氧", metrics = "dur_min,resistance,dist_km,calories", trackMode = "single")
        add("划船机", "有氧", metrics = "dur_min,resistance,dist_km,calories", trackMode = "single")
        add("跳绳", "有氧", metrics = "dur_min,reps,calories", trackMode = "single")
        add("农夫行走", "有氧", metrics = "weight,dist_m,dur_min", trackMode = "single")
        add("战绳", "有氧", metrics = "dur_sec")
        add("波比跳", "有氧", "compound", metrics = "reps")
        add("爬楼机", "有氧", metrics = "dur_min,resistance,calories", trackMode = "single")
        add("风阻单车", "有氧", metrics = "dur_min,resistance,dist_km,calories", trackMode = "single")
        add("壶铃摆荡", "有氧", "compound", metrics = "weight,reps")
    }
}
