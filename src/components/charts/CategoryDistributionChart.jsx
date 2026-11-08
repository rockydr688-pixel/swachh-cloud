import React from 'react';
import {
  PieChart,
  Pie,
  Cell,
  ResponsiveContainer,
  Tooltip,
} from 'recharts';
import Card from '../common/Card';

const CustomPieTooltip = ({ active, payload }) => {
  if (active && payload && payload.length) {
    const data = payload[0].payload;
    return (
      <div className="bg-slate-900 text-white p-2.5 rounded-lg shadow-xl text-xs border border-slate-700">
        <span className="font-bold text-slate-200">{data.name}</span>
        <div className="text-slate-400 mt-0.5">
          {data.value} GB ({data.percentage}%)
        </div>
      </div>
    );
  }
  return null;
};

export const CategoryDistributionChart = ({ data = [], className = '' }) => {
  return (
    <Card
      title="File Category Distribution"
      subtitle="Capacity allocation across media, docs & archives"
      className={className}
    >
      <div className="flex flex-col sm:flex-row items-center justify-between gap-6 pt-2">
        {/* Donut Chart */}
        <div className="h-60 w-60 relative shrink-0">
          <ResponsiveContainer width="100%" height="100%">
            <PieChart>
              <Tooltip content={<CustomPieTooltip />} />
              <Pie
                data={data}
                cx="50%"
                cy="50%"
                innerRadius={60}
                outerRadius={85}
                paddingAngle={4}
                dataKey="value"
              >
                {data.map((entry, index) => (
                  <Cell
                    key={`cell-${index}`}
                    fill={entry.color}
                    stroke="transparent"
                  />
                ))}
              </Pie>
            </PieChart>
          </ResponsiveContainer>
          {/* Center text */}
          <div className="absolute inset-0 flex flex-col items-center justify-center pointer-events-none">
            <span className="text-2xl font-black text-slate-800">842</span>
            <span className="text-[11px] font-semibold text-slate-400 uppercase tracking-wider">
              GB Total
            </span>
          </div>
        </div>

        {/* Legend List */}
        <div className="flex-1 w-full space-y-2">
          {data.map((item) => (
            <div
              key={item.name}
              className="flex items-center justify-between text-xs py-1 px-2 rounded-lg hover:bg-slate-50 transition-colors"
            >
              <div className="flex items-center gap-2">
                <span
                  className="w-2.5 h-2.5 rounded-sm shrink-0"
                  style={{ backgroundColor: item.color }}
                />
                <span className="font-medium text-slate-700">{item.name}</span>
              </div>
              <div className="flex items-center gap-2">
                <span className="font-bold text-slate-900">{item.value} GB</span>
                <span className="text-slate-400 font-mono text-[11px] w-8 text-right">
                  {item.percentage}%
                </span>
              </div>
            </div>
          ))}
        </div>
      </div>
    </Card>
  );
};

export default CategoryDistributionChart;

